package br.com.fotocastro.ui;

import br.com.fotocastro.infra.VendaDaoH2;
import br.com.fotocastro.model.PaymentMethod;
import br.com.fotocastro.model.Venda;
import br.com.fotocastro.template.TemplateManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller do Caixa - VERSÃO COM CARROSSEL DE VERDADE (FUNCIONANDO)
 */
public class CaixaController {

    private static final double CARTAO_PERCENT = 0.0533333333;
    private static final double PIX_PERCENT = 0.00;
    private static final double DIN_PERCENT = 0.00;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // UI geral
    @FXML private Label lblStatus;
    @FXML private Label lblCaixaStatus;
    @FXML private Button btnRemoverTemplate;

    // Carrossel
    @FXML private HBox carouselTrack;

    // Vendas
    @FXML private TextField txtDesc;
    @FXML private TextField txtPreco;
    @FXML private ChoiceBox<PaymentMethod> cbMetodo;
    @FXML private TableView<Venda> tblVendas;
    @FXML private TableColumn<Venda, LocalDateTime> colData;
    @FXML private TableColumn<Venda, String> colDesc;
    @FXML private TableColumn<Venda, Double> colBruto;
    @FXML private TableColumn<Venda, PaymentMethod> colMetodo;
    @FXML private TableColumn<Venda, Double> colTaxa;
    @FXML private TableColumn<Venda, Double> colLiq;
    @FXML private Label lblTotalVendas;

    // Dashboard HOJE
    @FXML private Label lblLiquidoHoje;
    @FXML private Label lblBrutoHoje;
    @FXML private Label lblTaxasHoje;
    @FXML private Label lblVariacaoDia;
    @FXML private Label lblDinheiroHoje, lblCartaoHoje, lblPixHoje;

    // Dashboard TOTAL
    @FXML private Label lblBrutoTotal, lblLiquidoTotal;

    // Dados
    private final ObservableList<Venda> vendas = FXCollections.observableArrayList();
    private VendaDaoH2 vendaDao;
    private TemplateManager templateManager;
    private Timer autoCloseTimer;
    private LocalDate ultimoDiaVerificado;

    // Carrossel
    private int currentIndex = 0;
    private final double CARD_WIDTH = 220;    // ajuste se o card for maior/menor
    private final double SPACING = 20;
    private final Duration TRANSITION_DURATION = Duration.millis(350);
    private int visibleCards = 4;

    @FXML
    public void initialize() {
        vendaDao = new VendaDaoH2();
        templateManager = new TemplateManager();
        ultimoDiaVerificado = LocalDate.now();

        templateManager.setOnVendaRealizada(() -> {
            recarregarVendas();
            atualizarDashboard();
            carregarTemplates();
            status("✓ Venda registrada via template");
        });

        cbMetodo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cbMetodo.setValue(PaymentMethod.DINHEIRO);

        colData.setCellValueFactory(new PropertyValueFactory<>("dataHora"));
        colData.setCellFactory(col -> new TableCell<Venda, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(TIME_FORMATTER));
            }
        });
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colBruto.setCellValueFactory(new PropertyValueFactory<>("valorBruto"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodo"));
        colTaxa.setCellValueFactory(new PropertyValueFactory<>("taxa"));
        colLiq.setCellValueFactory(new PropertyValueFactory<>("valorLiquido"));

        setupResizeListener();

        verificarFechamentoAutomatico();
        iniciarMonitoramentoDia();
        carregarTemplates();
        recarregarVendas();
        atualizarDashboard();
    }

    /* ===== CARROSSEL ===== */

    @FXML
    private void previousTemplate() {
        if (currentIndex > 0) {
            currentIndex--;
            animateCarousel();
        }
    }

    @FXML
    private void nextTemplate() {
        int total = carouselTrack.getChildren().size();
        if (currentIndex < total - visibleCards) {
            currentIndex++;
            animateCarousel();
        }
    }

    private void animateCarousel() {
        double targetX = -currentIndex * (CARD_WIDTH + SPACING);
        TranslateTransition tt = new TranslateTransition(TRANSITION_DURATION, carouselTrack);
        tt.setToX(targetX);
        tt.play();
    }

    private void updateCarouselPosition() {
        carouselTrack.setTranslateX(-currentIndex * (CARD_WIDTH + SPACING));
    }

    private void setupResizeListener() {
        carouselTrack.widthProperty().addListener((obs, old, newVal) -> {
            if (newVal.doubleValue() > 0) {
                // 80 = margem aproximada pros botões ◀ ▶ (40 cada lado)
                visibleCards = Math.max(1, (int) ((newVal.doubleValue() - 80) / (CARD_WIDTH + SPACING)));
                int maxIndex = Math.max(0, carouselTrack.getChildren().size() - visibleCards);
                if (currentIndex > maxIndex) {
                    currentIndex = maxIndex;
                    updateCarouselPosition();
                }
            }
        });
    }
    /* ===== TEMPLATES ===== */

    @FXML
    private void carregarTemplates() {
        templateManager.carregarTemplates(carouselTrack, btnRemoverTemplate);
        currentIndex = 0;
        updateCarouselPosition();
    }

    @FXML
    private void handleNovoTemplate() {
        templateManager.criarNovoTemplate().ifPresent(t -> {
            carregarTemplates();
            status("✓ Template criado");
        });
    }

    @FXML
    private void handleEditarTemplate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Nome do template para editar:");
        dialog.showAndWait().ifPresent(nome -> {
            var templates = new br.com.fotocastro.infra.TemplateDaoH2().listar();
            var alvo = templates.stream()
                    .filter(t -> t.getNome().equalsIgnoreCase(nome.trim()))
                    .findFirst()
                    .orElse(null);

            if (alvo == null) {
                alert("Template não encontrado.");
                return;
            }

            templateManager.editarTemplate(alvo).ifPresent(t -> {
                carregarTemplates();
                status("✓ Template atualizado");
            });
        });
    }

    @FXML
    private void handleRemoverTemplate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Nome do template para remover:");
        dialog.showAndWait().ifPresent(nome -> {
            var templates = new br.com.fotocastro.infra.TemplateDaoH2().listar();
            var alvo = templates.stream()
                    .filter(t -> t.getNome().equalsIgnoreCase(nome.trim()))
                    .findFirst()
                    .orElse(null);

            if (alvo == null) {
                alert("Template não encontrado.");
                return;
            }

            if (templateManager.removerTemplate(alvo)) {
                carregarTemplates();
                status("✓ Template removido");
            }
        });
    }

    /* ===== FECHAMENTO AUTOMÁTICO ===== */

    private void verificarFechamentoAutomatico() {
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);

        if (!vendaDao.caixaJaFechado(ontem)) {
            double bruto = vendaDao.somaBrutoPorDia(ontem);
            if (bruto > 0) {
                double taxas = vendaDao.somaTaxasPorDia(ontem);
                double liquido = vendaDao.somaLiquidoPorDia(ontem);
                double dinheiro = vendaDao.somaPorMetodoPorDia(PaymentMethod.DINHEIRO, ontem);
                double cartao = vendaDao.somaPorMetodoPorDia(PaymentMethod.CARTAO, ontem);
                double pix = vendaDao.somaPorMetodoPorDia(PaymentMethod.PIX, ontem);

                vendaDao.inserirFechamentoAutomatico(ontem, bruto, taxas, liquido, dinheiro, cartao, pix);

                Platform.runLater(() -> {
                    lblCaixaStatus.setText("⚠️ Caixa de " + ontem + " fechado automaticamente");
                    lblCaixaStatus.setStyle("-fx-text-fill: #f59e0b;");
                });
            }
        }
    }

    private void iniciarMonitoramentoDia() {
        autoCloseTimer = new Timer(true);
        autoCloseTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalDate agora = LocalDate.now();
                if (!agora.equals(ultimoDiaVerificado)) {
                    Platform.runLater(() -> {
                        verificarFechamentoAutomatico();
                        ultimoDiaVerificado = agora;
                        recarregarVendas();
                        atualizarDashboard();
                    });
                }
            }
        }, 60000, 60000);
    }

    /* ===== VENDAS ===== */

    @FXML
    private void handleAdicionarVenda() {
        String desc = txtDesc.getText() == null ? "" : txtDesc.getText().trim();
        double preco;
        try {
            preco = Double.parseDouble(txtPreco.getText().replace(",", ".").trim());
        } catch (Exception e) {
            alert("Preço inválido.");
            return;
        }
        if (desc.isBlank()) {
            alert("Descrição obrigatória.");
            return;
        }

        PaymentMethod metodo = cbMetodo.getValue();
        inserirVenda(desc, preco, metodo);

        txtDesc.clear();
        txtPreco.clear();
    }

    private void inserirVenda(String descricao, double valorBruto, PaymentMethod metodo) {
        double taxa = calcularTaxa(valorBruto, metodo);
        Venda v = new Venda();
        v.setDataHora(LocalDateTime.now());
        v.setDescricao(descricao);
        v.setValorBruto(valorBruto);
        v.setMetodo(metodo);
        v.setTaxa(taxa);
        v.setValorLiquido(Math.max(0.0, valorBruto - taxa));

        vendaDao.inserir(v);
        recarregarVendas();
        atualizarDashboard();
        status("✓ Venda adicionada");
    }

    @FXML
    private void handleEditarVenda() {
        Venda sel = tblVendas.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog d1 = new TextInputDialog(sel.getDescricao());
        d1.setHeaderText("Editar descrição");
        var o1 = d1.showAndWait();
        if (o1.isEmpty()) return;

        TextInputDialog d2 = new TextInputDialog(String.format("%.2f", sel.getValorBruto()));
        d2.setHeaderText("Editar valor bruto (R$)");
        var o2 = d2.showAndWait();
        if (o2.isEmpty()) return;

        ChoiceDialog<PaymentMethod> d3 = new ChoiceDialog<>(sel.getMetodo(), PaymentMethod.values());
        d3.setHeaderText("Editar método de pagamento");
        var o3 = d3.showAndWait();
        if (o3.isEmpty()) return;

        double bruto;
        try {
            bruto = Double.parseDouble(o2.get().replace(",", ".").trim());
        } catch (Exception e) {
            alert("Valor bruto inválido.");
            return;
        }

        PaymentMethod metodo = o3.get();
        double taxa = calcularTaxa(bruto, metodo);

        sel.setDescricao(o1.get().trim());
        sel.setValorBruto(bruto);
        sel.setMetodo(metodo);
        sel.setTaxa(taxa);
        sel.setValorLiquido(Math.max(0.0, bruto - taxa));

        vendaDao.atualizar(sel);
        recarregarVendas();
        atualizarDashboard();
        status("✓ Venda atualizada");
    }

    @FXML
    private void handleRemoverVenda() {
        Venda sel = tblVendas.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Remover venda selecionada?", ButtonType.OK, ButtonType.CANCEL);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                vendaDao.remover(sel.getId());
                recarregarVendas();
                atualizarDashboard();
                status("✓ Venda removida");
            }
        });
    }

    @FXML
    private void handleLimparVendasHoje() {
        Alert confirm = new Alert(Alert.AlertType.WARNING,
                "Deseja realmente limpar TODAS as vendas de hoje?\n\nEsta ação NÃO pode ser desfeita!",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("⚠️ Confirmar Limpeza");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                vendaDao.limparVendasHoje();
                recarregarVendas();
                atualizarDashboard();
                status("✓ Vendas de hoje limpas");
            }
        });
    }

    @FXML
    private void handleFecharCaixa() {
        LocalDate hoje = LocalDate.now();

        if (vendaDao.caixaJaFechado(hoje)) {
            alert("O caixa de hoje já foi fechado.");
            return;
        }

        double bruto = vendaDao.somaBrutoHoje();
        double taxas = vendaDao.somaTaxasHoje();
        double liquido = vendaDao.somaLiquidoHoje();
        double dinheiro = vendaDao.somaPorMetodoHoje(PaymentMethod.DINHEIRO);
        double cartao = vendaDao.somaPorMetodoHoje(PaymentMethod.CARTAO);
        double pix = vendaDao.somaPorMetodoHoje(PaymentMethod.PIX);

        String resumo = "═══════════════════════════════\n" +
                "  FECHAMENTO DE CAIXA - " + hoje + "\n" +
                "═══════════════════════════════\n\n" +
                "💰 Bruto:     " + fmt(bruto) + "\n" +
                "💸 Taxas:     " + fmt(taxas) + "\n" +
                "✅ Líquido:   " + fmt(liquido) + "\n\n" +
                "───────────────────────────────\n\n" +
                "💵 Dinheiro:  " + fmt(dinheiro) + "\n" +
                "💳 Cartão:    " + fmt(cartao) + "\n" +
                "📱 PIX:       " + fmt(pix) + "\n\n" +
                "═══════════════════════════════\n" +
                "Deseja fechar o caixa?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, resumo, ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Fechamento de Caixa");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                vendaDao.inserirFechamento(hoje, bruto, taxas, liquido, dinheiro, cartao, pix);
                lblCaixaStatus.setText("🔒 Caixa Fechado");
                lblCaixaStatus.setStyle("-fx-text-fill: #ef4444;");
                status("✓ Caixa fechado com sucesso");

                vendaDao.limparVendasHoje();
                recarregarVendas();
                atualizarDashboard();
            }
        });
    }

    private void recarregarVendas() {
        vendas.setAll(vendaDao.listarHoje());
        tblVendas.setItems(vendas);

        if (lblTotalVendas != null) {
            lblTotalVendas.setText(vendas.size() + " vendas");
        }
    }

    private void atualizarDashboard() {
        double brutoHoje = vendaDao.somaBrutoHoje();
        double taxasHoje = vendaDao.somaTaxasHoje();
        double liqHoje = vendaDao.somaLiquidoHoje();

        lblBrutoHoje.setText(fmt(brutoHoje));
        lblTaxasHoje.setText(fmt(taxasHoje));
        lblLiquidoHoje.setText(fmt(liqHoje));

        double dinHoje = vendaDao.somaPorMetodoHoje(PaymentMethod.DINHEIRO);
        double cartHoje = vendaDao.somaPorMetodoHoje(PaymentMethod.CARTAO);
        double pixHoje = vendaDao.somaPorMetodoHoje(PaymentMethod.PIX);

        lblDinheiroHoje.setText(fmt(dinHoje));
        lblCartaoHoje.setText(fmt(cartHoje));
        lblPixHoje.setText(fmt(pixHoje));

        LocalDate ontem = LocalDate.now().minusDays(1);
        double liqOntem = vendaDao.somaLiquidoPorDia(ontem);

        if (liqOntem > 0) {
            double variacao = ((liqHoje - liqOntem) / liqOntem) * 100;
            String sinal = variacao >= 0 ? "📈 +" : "📉 ";
            lblVariacaoDia.setText(String.format("%s%.1f%%", sinal, Math.abs(variacao)));
            lblVariacaoDia.setStyle("-fx-text-fill: " + (variacao >= 0 ? "#10b981" : "#ef4444") + "; -fx-font-weight: 800;");
        } else {
            lblVariacaoDia.setText("🆕 Primeiro dia");
            lblVariacaoDia.setStyle("-fx-text-fill: white; -fx-font-weight: 800;");
        }

        lblBrutoTotal.setText(fmt(vendaDao.somaBruto()));
        lblLiquidoTotal.setText(fmt(vendaDao.somaLiquido()));
    }

    /* ===== UTIL ===== */

    private double calcularTaxa(double valor, PaymentMethod metodo) {
        double p = switch (metodo) {
            case CARTAO -> CARTAO_PERCENT;
            case PIX -> PIX_PERCENT;
            default -> DIN_PERCENT;
        };
        return round2(valor * p);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String fmt(double v) {
        return "R$ " + String.format("%.2f", v);
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private void status(String s) {
        if (lblStatus != null) lblStatus.setText(s);
    }

    public void cleanup() {
        if (autoCloseTimer != null) {
            autoCloseTimer.cancel();
        }
    }
}