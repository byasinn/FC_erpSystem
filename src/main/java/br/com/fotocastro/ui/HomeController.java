package br.com.fotocastro.ui;

import br.com.fotocastro.infra.VendaDaoH2;
import br.com.fotocastro.infra.VendaDaoH2.FechamentoResumo; // Import da classe correta
import br.com.fotocastro.utils.PdfExporter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class HomeController {

    @FXML private Label lblStatusCaixa;
    @FXML private Button btnAbrirCaixa;
    @FXML private Button btnFecharCaixa;

    @FXML private TabPane mainTabs;

    // Aba Caixas Anteriores
    @FXML private TableView<FechamentoResumo> tableFechamentos;
    @FXML private Button btnVerDetalhes;
    @FXML private Button btnExportarPDF;

    // Aba Avisos
    @FXML private ListView<String> listAvisos;
    @FXML private Button btnLimparAvisos;

    // Aba Relatórios
    @FXML private Button btnRelHoje;
    @FXML private Button btnRelSemana;
    @FXML private Button btnRelMes;
    @FXML private Button btnTopTemplates;
    @FXML private Button btnRelMetodos;
    @FXML private Label lblRelatorioStatus;
    @FXML private VBox relatorioContent;

    private final VendaDaoH2 vendaDao = new VendaDaoH2();

    @FXML
    private void initialize() {
        atualizarStatusCaixa();
        configurarTableFechamentos();
        carregarFechamentos(); // Agora usa DAO real (comente se quiser exemplo)
        carregarAvisosExemplo();

        btnVerDetalhes.setOnAction(e -> verDetalhesFechamento());
        btnExportarPDF.setOnAction(e -> exportarPDF());
        btnLimparAvisos.setOnAction(e -> limparAvisos());

        btnRelHoje.setOnAction(e -> mostrarRelatorio("Vendas Hoje"));
        btnRelSemana.setOnAction(e -> mostrarRelatorio("Vendas Semana"));
        // ... outros botões iguais

        btnAbrirCaixa.setVisible(false);
        btnFecharCaixa.setVisible(false);
    }

    private void atualizarStatusCaixa() {
        boolean fechadoHoje = vendaDao.caixaJaFechado(LocalDate.now());
        if (fechadoHoje) {
            lblStatusCaixa.setText("FECHADO");
            lblStatusCaixa.getStyleClass().setAll("status-closed");
        } else {
            lblStatusCaixa.setText("ABERTO");
            lblStatusCaixa.getStyleClass().setAll("status-open");
        }
    }

    private void configurarTableFechamentos() {
        tableFechamentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Todas as colunas declaradas AQUI dentro do método (não como campos da classe)
        TableColumn<FechamentoResumo, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colData.setPrefWidth(100);

        TableColumn<FechamentoResumo, String> colFechadoEm = new TableColumn<>("Fechado em");
        colFechadoEm.setCellValueFactory(new PropertyValueFactory<>("fechadoEm"));
        colFechadoEm.setPrefWidth(140);

        TableColumn<FechamentoResumo, String> colLiquido = new TableColumn<>("Líquido");
        colLiquido.setCellValueFactory(new PropertyValueFactory<>("liquido"));
        colLiquido.setPrefWidth(100);

        TableColumn<FechamentoResumo, String> colDiferenca = new TableColumn<>("Diferença");
        colDiferenca.setCellValueFactory(new PropertyValueFactory<>("diferenca"));
        colDiferenca.setPrefWidth(90);

        TableColumn<FechamentoResumo, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(100);

        TableColumn<FechamentoResumo, String> colObs = new TableColumn<>("Observação");
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacao"));

        tableFechamentos.getColumns().addAll(colData, colFechadoEm, colLiquido, colDiferenca, colTipo, colObs);
    }

    private void carregarFechamentos() {
        // Usa o DAO real
        var lista = vendaDao.listarFechamentos(50); // últimos 50
        tableFechamentos.setItems(FXCollections.observableArrayList(lista));
    }

    private void carregarFechamentosExemplo() {
        // Só use isso temporariamente se o DAO não tiver dados ainda
        // comente quando carregarFechamentos() funcionar
    }

    private void carregarAvisosExemplo() {
        listAvisos.getItems().addAll(
                "⚠️ Estoque de Papel Fotográfico 10x15 abaixo do mínimo (restam 18 unidades)",
                "📉 Última semana: vendas 35% abaixo da média histórica",
                "🛠️ Manutenção impressora agendada para sexta-feira",
                "💰 Pagamento fornecedor X vence em 3 dias"
        );
    }

    private void verDetalhesFechamento() {
        FechamentoResumo selected = tableFechamentos.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione um fechamento!").showAndWait();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Data: ").append(selected.getData()).append("\n");
        sb.append("Fechado em: ").append(selected.getFechadoEm()).append("\n\n");
        sb.append("Bruto: ").append(selected.getBruto()).append("\n");
        sb.append("Líquido: ").append(selected.getLiquido()).append("\n");
        sb.append("Total pagamentos: ").append(selected.getTotalPagamentos()).append("\n\n");
        sb.append("Valor contado: R$ ").append(String.format("%.2f", selected.valorContado)).append("\n");
        sb.append("Diferença: ").append(selected.getDiferenca()).append("\n\n");
        sb.append("Observação: ").append(selected.getObservacao()).append("\n");
        sb.append("Tipo: ").append(selected.getTipo());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalhes do Fechamento");
        alert.setHeaderText("Fechamento de " + selected.getData());
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    private void exportarPDF() {
        FechamentoResumo selected = tableFechamentos.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione um fechamento primeiro!").showAndWait();
            return;
        }

        String fileName = "fechamento_" + selected.getData().replace("/", "-") + ".pdf";
        String path = System.getProperty("user.home") + "/Desktop/" + fileName;

        try {
            PdfExporter.exportFechamentoToPdf(selected, path);
            new Alert(Alert.AlertType.INFORMATION, "PDF exportado com sucesso!\nSalvo em: " + path).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Falha ao gerar PDF: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    private void limparAvisos() {
        listAvisos.getItems().clear();
        new Alert(Alert.AlertType.INFORMATION, "Avisos limpos.").show();
    }

    private void mostrarRelatorio(String tipo) {
        lblRelatorioStatus.setText("Gerando: " + tipo + "...");
        Label placeholder = new Label("Relatório " + tipo + " em desenvolvimento");
        relatorioContent.getChildren().setAll(placeholder);
    }
}