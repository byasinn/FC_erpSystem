package br.com.fotocastro.ui;

import br.com.fotocastro.infra.EstoqueDaoH2;
import br.com.fotocastro.model.ItemEstoque;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;

public class EstoqueController {

    @FXML private TableView<ItemEstoque> tblEstoque;
    @FXML private TableColumn<ItemEstoque, String> colNome;
    @FXML private TableColumn<ItemEstoque, String> colCor;
    @FXML private TableColumn<ItemEstoque, String> colTam;
    @FXML private TableColumn<ItemEstoque, Integer> colQtd;
    @FXML private TableColumn<ItemEstoque, Double> colCusto;
    @FXML private TableColumn<ItemEstoque, Double> colUnit;

    private final ObservableList<ItemEstoque> dados = FXCollections.observableArrayList();
    private EstoqueDaoH2 dao;

    @FXML
    public void initialize() {
        dao = new EstoqueDaoH2();

        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCor.setCellValueFactory(new PropertyValueFactory<>("cor"));
        colTam.setCellValueFactory(new PropertyValueFactory<>("tamanho"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colCusto.setCellValueFactory(new PropertyValueFactory<>("custoTotal"));
        colUnit.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getCustoUnitario()));

        recarregar();
    }

    private void recarregar() {
        dados.setAll(dao.listarTodos());
        tblEstoque.setItems(dados);
    }

    @FXML
    private void handleNovo() {
        ItemEstoque novo = openDialogAndGet(null, false);
        if (novo == null) return;
        dao.adicionar(novo);
        recarregar();
    }

    @FXML
    private void handleEditar() {
        ItemEstoque sel = tblEstoque.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        ItemEstoque editado = openDialogAndGet(copy(sel), false);
        if (editado == null) return;

        // Atualiza cadastro: nome/cor/tamanho
        sel.setNome(editado.getNome());
        sel.setCor(editado.getCor());
        sel.setTamanho(editado.getTamanho());
        // opcional: permitir edição direta de quantidade/custo_total
        sel.setQuantidade(editado.getQuantidade());
        sel.setCustoTotal(editado.getCustoTotal());

        dao.atualizarCadastro(sel);
        // se mudou quantidade/custo_total pelo diálogo, persiste com uma atualização adicional:
        // (reuso da atualizarCadastro só faz nome/cor/tamanho — então pode criar outro método, ou,
        // aqui por simplicidade, removo e insiro novamente. Melhor criar um método dedicado:)
        // Vamos criar um método rápido:
        atualizarQuantidadeECusto(sel.getId(), sel.getQuantidade(), sel.getCustoTotal());

        recarregar();
    }

    @FXML
    private void handleEntradaLote() {
        ItemEstoque sel = tblEstoque.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        ItemEstoque delta = openDialogAndGet(copy(sel), true);
        if (delta == null) return;
        if (delta.getQuantidade() <= 0 || delta.getCustoTotal() < 0) return;
        dao.entradaLote(sel.getId(), delta.getQuantidade(), delta.getCustoTotal());
        recarregar();
    }

    @FXML
    private void handleSaida() {
        ItemEstoque sel = tblEstoque.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog d = new TextInputDialog("1");
        d.setHeaderText("Saída/consumo de estoque");
        d.setContentText("Quantidade a dar baixa:");
        d.showAndWait().ifPresent(q -> {
            try {
                int qtd = Integer.parseInt(q.trim());
                if (qtd > 0) {
                    dao.saidaConsumo(sel.getId(), qtd);
                    recarregar();
                }
            } catch (Exception ignored) {}
        });
    }

    @FXML
    private void handleRemover() {
        ItemEstoque sel = tblEstoque.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Remover item do estoque?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar remoção");
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                dao.remover(sel.getId());
                recarregar();
            }
        });
    }

    // ===== Helpers =====

    private ItemEstoque openDialogAndGet(ItemEstoque base, boolean entradaDeLote) {
        try {
            URL url = getClass().getResource("/ui/EstoqueEditDialog.fxml");
            if (url == null) {
                new Alert(Alert.AlertType.ERROR, "FXML não encontrado: /ui/EstoqueEditDialog.fxml").showAndWait();
                return null;
            }

            FXMLLoader fx = new FXMLLoader(url);
            DialogPane pane = fx.load();
            EstoqueEditController ctrl = fx.getController();
            ctrl.setItem(base, entradaDeLote);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(entradaDeLote ? "Entrada de Lote" : (base == null ? "Novo Produto" : "Editar Produto"));
            dialog.setDialogPane(pane);

            // pega o botão cujo ButtonData é OK_DONE (texto pode ser “Salvar”)
            var okType = pane.getButtonTypes().stream()
                    .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst().orElse(null);
            if (okType != null) {
                var okBtn = (Button) pane.lookupButton(okType);
                if (okBtn != null) {
                    okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                        try {
                            ctrl.getResultado(); // valida campos
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                            ev.consume(); // não fecha o diálogo
                        }
                    });
                }
            }

            var res = dialog.showAndWait();
            if (res.isPresent() && res.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return ctrl.getResultado();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erro ao abrir diálogo: " + e.getMessage()).showAndWait();
        }
        return null;
    }

    private ItemEstoque copy(ItemEstoque i) {
        ItemEstoque c = new ItemEstoque();
        c.setId(i.getId());
        c.setNome(i.getNome());
        c.setCor(i.getCor());
        c.setTamanho(i.getTamanho());
        c.setQuantidade(i.getQuantidade());
        c.setCustoTotal(i.getCustoTotal());
        return c;
    }

    private void atualizarQuantidadeECusto(Long id, int novaQtd, double novoCusto) {
        // método pequeno e direto no lugar: UPDATE total
        final String sql = "UPDATE estoque SET quantidade=?, custo_total=? WHERE id=?";
        try (var c = java.sql.DriverManager.getConnection("jdbc:h2:file:./data/fotocastro;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, novaQtd);
            ps.setDouble(2, novoCusto);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
