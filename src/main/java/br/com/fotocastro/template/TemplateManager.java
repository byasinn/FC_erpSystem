package br.com.fotocastro.template;

import br.com.fotocastro.infra.EstoqueDaoH2;
import br.com.fotocastro.infra.TemplateDaoH2;
import br.com.fotocastro.infra.VendaDaoH2;
import br.com.fotocastro.model.PaymentMethod;
import br.com.fotocastro.model.TemplateVenda;
import br.com.fotocastro.model.Venda;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.geometry.Pos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * Gerenciador central de templates.
 * Responsável por carregar, exibir e processar vendas via template.
 */
public class TemplateManager {

    private static final Logger logger = Logger.getLogger(TemplateManager.class.getName());

    // Taxas
    private static final double CARTAO_PERCENT = 0.0533333333;
    private static final double PIX_PERCENT = 0.00;
    private static final double DIN_PERCENT = 0.00;

    private final TemplateDaoH2 templateDao;
    private final EstoqueDaoH2 estoqueDao;
    private final VendaDaoH2 vendaDao;

    private Runnable onVendaRealizada;

    public TemplateManager() {
        this.templateDao = new TemplateDaoH2();
        this.estoqueDao = new EstoqueDaoH2();
        this.vendaDao = new VendaDaoH2();
    }

    // ========== CARREGAR TEMPLATES ==========

    public void carregarTemplates(HBox track, Button btnRemover) {
        track.getChildren().clear();

        List<TemplateVenda> templates = templateDao.listarComEstoqueInfo();

        if (templates.isEmpty()) {
            Label lblVazio = new Label("Nenhum template cadastrado.\nClique em '＋ Novo' para criar.");
            lblVazio.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-alignment: center;");
            track.getChildren().add(lblVazio);
            return;
        }

        for (TemplateVenda template : templates) {
            TemplateCard card = new TemplateCard(template);
            card.setOnClick(() -> venderTemplate(template));
            configurarDragParaRemocao(card, btnRemover, track);
            track.getChildren().add(card);
        }
    }

    private void configurarDragParaRemocao(TemplateCard card, Button btnRemover, HBox track) {
        // Inicia o drag
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("template-card");
            db.setContent(content);
            card.setOpacity(0.6);
            event.consume();
        });

        // Feedback no botão
        btnRemover.setOnDragOver(event -> {
            if (event.getGestureSource() != btnRemover && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
                btnRemover.setStyle("-fx-background-color: #dc2626; -fx-scale-x: 1.15; -fx-scale-y: 1.15;");
            }
            event.consume();
        });

        btnRemover.setOnDragExited(event -> {
            btnRemover.setStyle(""); // volta ao normal (ou define no CSS)
        });

        // Drop no botão = remover
        btnRemover.setOnDragDropped(event -> {
            if (event.getDragboard().hasString() && event.getGestureSource() instanceof TemplateCard sourceCard) {
                TemplateVenda template = sourceCard.getTemplate();

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Remover o template \"" + template.getNome() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirmar Remoção");

                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        templateDao.remover(template.getId());
                        track.getChildren().remove(sourceCard);
                        showInfo("Template removido!");
                    }
                });

                event.setDropCompleted(true);
            }
            event.consume();
        });

        // Restaura opacidade ao finalizar
        card.setOnDragDone(event -> card.setOpacity(1.0));
    }

    // ========== CRIAR/EDITAR/REMOVER ==========

    public Optional<TemplateVenda> criarNovoTemplate() {
        TemplateDialog dialog = new TemplateDialog();
        Optional<TemplateVenda> resultado = dialog.showAndWait();

        resultado.ifPresent(template -> {
            try {
                Long id = templateDao.inserir(template);
                template.setId(id);
                logger.info("Template criado com sucesso: " + template.getNome());
                showInfo("Template criado com sucesso!");
            } catch (Exception e) {
                logger.severe("Erro ao criar template: " + e.getMessage());
                showError("Erro ao criar template: " + e.getMessage());
            }
        });

        return resultado;
    }

    public Optional<TemplateVenda> editarTemplate(TemplateVenda template) {
        if (template == null) return Optional.empty();

        TemplateDialog dialog = new TemplateDialog(template);
        Optional<TemplateVenda> resultado = dialog.showAndWait();

        resultado.ifPresent(templateEditado -> {
            try {
                templateDao.atualizar(templateEditado);
                logger.info("Template atualizado: " + templateEditado.getNome());
                showInfo("Template atualizado com sucesso!");
            } catch (Exception e) {
                logger.severe("Erro ao atualizar template: " + e.getMessage());
                showError("Erro ao atualizar template: " + e.getMessage());
            }
        });

        return resultado;
    }

    public boolean removerTemplate(TemplateVenda template) {
        if (template == null) return false;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente remover o template \"" + template.getNome() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmar Remoção");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                templateDao.remover(template.getId());
                logger.info("Template removido: " + template.getNome());
                showInfo("Template removido com sucesso!");
                return true;
            } catch (Exception e) {
                logger.severe("Erro ao remover template: " + e.getMessage());
                showError("Erro ao remover template: " + e.getMessage());
            }
        }

        return false;
    }

    // ========== PROCESSAR VENDA ==========

    public void venderTemplate(TemplateVenda template) {
        int quantidade = perguntarQuantidade(template);
        if (quantidade <= 0) return;

        if (template.temEstoqueVinculado()) {
            int necessario = template.calcularConsumoEstoque(quantidade);
            if (!template.temEstoqueSuficiente(quantidade)) {
                showError(String.format(
                        "Estoque insuficiente!\n\nNecessário: %d unidades\nDisponível: %d unidades",
                        necessario, template.getEstoqueDisponivel()));
                return;
            }
        }

        PaymentMethod metodo = perguntarMetodoPagamento();
        if (metodo == null) return;

        double valorBruto = template.getPreco() * quantidade;
        double taxa = calcularTaxa(valorBruto, metodo);
        double valorLiquido = Math.max(0.0, valorBruto - taxa);

        String confirmacao = String.format(
                "Confirmar venda?\n\n%s × %d\nValor: R$ %.2f\nMétodo: %s\nTaxa: R$ %.2f\nLíquido: R$ %.2f",
                template.getDescricaoCompleta(), quantidade, valorBruto, metodo, taxa, valorLiquido);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, confirmacao, ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirmar Venda");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            Venda venda = new Venda();
            venda.setDataHora(LocalDateTime.now());
            venda.setDescricao(String.format("%s × %d", template.getDescricaoCompleta(), quantidade));
            venda.setValorBruto(valorBruto);
            venda.setMetodo(metodo);
            venda.setTaxa(taxa);
            venda.setValorLiquido(valorLiquido);

            vendaDao.inserir(venda);

            if (template.temEstoqueVinculado()) {
                int consumo = template.calcularConsumoEstoque(quantidade);
                estoqueDao.saidaConsumo(template.getEstoqueItemId(), consumo);
            }

            logger.info("Venda realizada: " + venda.getDescricao());
            showInfo("Venda registrada com sucesso!");

            if (onVendaRealizada != null) {
                onVendaRealizada.run();
            }

        } catch (Exception e) {
            logger.severe("Erro ao processar venda: " + e.getMessage());
            showError("Erro ao processar venda: " + e.getMessage());
        }
    }

    // ========== DIALOGS AUXILIARES ==========

    private int perguntarQuantidade(TemplateVenda template) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Quantidade");
        dialog.setHeaderText("Venda: " + template.getNome());
        dialog.setContentText("Quantidade:");

        return dialog.showAndWait().map(str -> {
            try {
                int q = Integer.parseInt(str.trim());
                return q > 0 ? q : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }).orElse(0);
    }

    private PaymentMethod perguntarMetodoPagamento() {
        ChoiceDialog<PaymentMethod> dialog = new ChoiceDialog<>(PaymentMethod.DINHEIRO, PaymentMethod.values());
        dialog.setTitle("Método de Pagamento");
        dialog.setHeaderText("Selecione a forma de pagamento");
        dialog.setContentText("Método:");
        return dialog.showAndWait().orElse(null);
    }

    private double calcularTaxa(double valor, PaymentMethod metodo) {
        double p = switch (metodo) {
            case CARTAO -> CARTAO_PERCENT;
            case PIX -> PIX_PERCENT;
            case DINHEIRO -> DIN_PERCENT;
        };
        return Math.round(valor * p * 100.0) / 100.0;
    }

    // ========== HELPERS ==========

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public void setOnVendaRealizada(Runnable callback) {
        this.onVendaRealizada = callback;
    }
}