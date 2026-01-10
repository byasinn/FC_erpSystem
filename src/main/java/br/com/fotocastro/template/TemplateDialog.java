package br.com.fotocastro.template;

import br.com.fotocastro.infra.EstoqueDaoH2;
import br.com.fotocastro.model.ItemEstoque;
import br.com.fotocastro.model.TemplateVenda;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

/**
 * Dialog para criar ou editar templates de venda.
 * Campos: nome, preço, tamanho, ícone, tag, item de estoque, quantidade de uso.
 */
public class TemplateDialog {
    
    private final Dialog<TemplateVenda> dialog;
    private final TemplateVenda template;
    private final boolean isEdit;
    
    // Campos do formulário
    private TextField txtNome;
    private TextField txtPreco;
    private TextField txtTamanho;
    private ChoiceBox<TemplateIcon> cbIcone;
    private TextField txtTag;
    private ComboBox<ItemEstoque> cbEstoque;
    private Spinner<Integer> spnQuantidade;
    
    private EstoqueDaoH2 estoqueDao;
    
    /**
     * Cria dialog para novo template
     */
    public TemplateDialog() {
        this(null);
    }
    
    /**
     * Cria dialog para editar template existente
     */
    public TemplateDialog(TemplateVenda template) {
        this.template = template;
        this.isEdit = template != null;
        this.dialog = new Dialog<>();
        this.estoqueDao = new EstoqueDaoH2();
        
        setupDialog();
    }
    
    private void setupDialog() {
        dialog.setTitle(isEdit ? "Editar Template" : "Novo Template");
        dialog.setHeaderText(isEdit ? "Edite as informações do template" : "Crie um novo template de venda");
        
        // Botões
        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, btnCancelar);
        
        // Grid para os campos
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        
        int row = 0;
        
        // Nome (obrigatório)
        grid.add(new Label("Nome:*"), 0, row);
        txtNome = new TextField();
        txtNome.setPromptText("Ex: Foto 10x15");
        if (isEdit) txtNome.setText(template.getNome());
        grid.add(txtNome, 1, row++);
        
        // Preço (obrigatório)
        grid.add(new Label("Preço (R$):*"), 0, row);
        txtPreco = new TextField();
        txtPreco.setPromptText("Ex: 5.00");
        if (isEdit) txtPreco.setText(String.format("%.2f", template.getPreco()));
        grid.add(txtPreco, 1, row++);
        
        // Tamanho (opcional)
        grid.add(new Label("Tamanho:"), 0, row);
        txtTamanho = new TextField();
        txtTamanho.setPromptText("Ex: 10x15, A4 (opcional)");
        if (isEdit && template.getTamanho() != null) {
            txtTamanho.setText(template.getTamanho());
        }
        grid.add(txtTamanho, 1, row++);
        
        // Ícone
        grid.add(new Label("Ícone:"), 0, row);
        cbIcone = new ChoiceBox<>(FXCollections.observableArrayList(TemplateIcon.values()));
        cbIcone.setValue(isEdit ? template.getIcone() : TemplateIcon.PHOTO);
        cbIcone.setConverter(new javafx.util.StringConverter<TemplateIcon>() {
            @Override
            public String toString(TemplateIcon icon) {
                return icon != null ? icon.getDisplay() : "";
            }
            
            @Override
            public TemplateIcon fromString(String string) {
                return null;
            }
        });
        grid.add(cbIcone, 1, row++);
        
        // Tag/Categoria (opcional)
        grid.add(new Label("Categoria:"), 0, row);
        txtTag = new TextField();
        txtTag.setPromptText("Ex: Revelação, Documento (opcional)");
        if (isEdit && template.getTag() != null) {
            txtTag.setText(template.getTag());
        }
        grid.add(txtTag, 1, row++);
        
        // Separador
        Separator sep = new Separator();
        GridPane.setColumnSpan(sep, 2);
        grid.add(sep, 0, row++);
        
        // Label explicativo
        Label lblEstoque = new Label("Vinculação com Estoque (opcional):");
        lblEstoque.setStyle("-fx-font-weight: bold;");
        GridPane.setColumnSpan(lblEstoque, 2);
        grid.add(lblEstoque, 0, row++);
        
        // Item de estoque (opcional)
        grid.add(new Label("Produto:"), 0, row);
        cbEstoque = new ComboBox<>();
        cbEstoque.setPromptText("Selecione (opcional)");
        cbEstoque.setPrefWidth(200);
        
        // Carrega itens de estoque
        List<ItemEstoque> itensEstoque = estoqueDao.listarTodos();
        cbEstoque.getItems().add(null); // Opção "nenhum"
        cbEstoque.getItems().addAll(itensEstoque);
        
        cbEstoque.setConverter(new javafx.util.StringConverter<ItemEstoque>() {
            @Override
            public String toString(ItemEstoque item) {
                if (item == null) return "(Nenhum)";
                return String.format("%s - %s (Qtd: %d)", 
                    item.getNome(), 
                    item.getTamanho() != null ? item.getTamanho() : "N/A",
                    item.getQuantidade());
            }
            
            @Override
            public ItemEstoque fromString(String string) {
                return null;
            }
        });
        
        if (isEdit && template.getEstoqueItemId() != null) {
            Optional<ItemEstoque> itemOpt = estoqueDao.buscarPorId(template.getEstoqueItemId());
            itemOpt.ifPresent(cbEstoque::setValue);
        }
        
        grid.add(cbEstoque, 1, row++);
        
        // Quantidade de uso
        grid.add(new Label("Qtd consumida:"), 0, row);
        spnQuantidade = new Spinner<>(1, 100, 1);
        spnQuantidade.setEditable(true);
        if (isEdit) spnQuantidade.getValueFactory().setValue(template.getQuantidadeUso());
        grid.add(spnQuantidade, 1, row++);
        
        // Nota explicativa
        Label lblNota = new Label(
            "Ao vender este template, a quantidade\n" +
            "será automaticamente descontada do estoque."
        );
        lblNota.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
        lblNota.setWrapText(true);
        GridPane.setColumnSpan(lblNota, 2);
        grid.add(lblNota, 0, row++);
        
        dialog.getDialogPane().setContent(grid);
        
        // Validação ao salvar
        Button btnSalvarNode = (Button) dialog.getDialogPane().lookupButton(
            dialog.getDialogPane().getButtonTypes().stream()
                .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .findFirst()
                .orElse(null)
        );
        
        if (btnSalvarNode != null) {
            btnSalvarNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!validarCampos()) {
                    event.consume();
                }
            });
        }
        
        // Converter resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return criarTemplate();
            }
            return null;
        });
    }
    
    private boolean validarCampos() {
        String nome = txtNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showError("Nome é obrigatório");
            return false;
        }
        
        String precoStr = txtPreco.getText();
        if (precoStr == null || precoStr.trim().isEmpty()) {
            showError("Preço é obrigatório");
            return false;
        }
        
        try {
            double preco = Double.parseDouble(precoStr.replace(",", "."));
            if (preco < 0) {
                showError("Preço não pode ser negativo");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Preço inválido");
            return false;
        }
        
        return true;
    }
    
    private TemplateVenda criarTemplate() {
        TemplateVenda t = isEdit ? template : new TemplateVenda();
        
        t.setNome(txtNome.getText().trim());
        t.setPreco(Double.parseDouble(txtPreco.getText().replace(",", ".").trim()));
        
        String tamanho = txtTamanho.getText();
        t.setTamanho(tamanho != null && !tamanho.trim().isEmpty() ? tamanho.trim() : null);
        
        t.setIcone(cbIcone.getValue());
        
        String tag = txtTag.getText();
        t.setTag(tag != null && !tag.trim().isEmpty() ? tag.trim() : null);
        
        ItemEstoque itemEstoque = cbEstoque.getValue();
        t.setEstoqueItemId(itemEstoque != null ? itemEstoque.getId() : null);
        
        t.setQuantidadeUso(spnQuantidade.getValue());
        
        return t;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Validação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Exibe o dialog e retorna o template criado/editado
     */
    public Optional<TemplateVenda> showAndWait() {
        return dialog.showAndWait();
    }
}