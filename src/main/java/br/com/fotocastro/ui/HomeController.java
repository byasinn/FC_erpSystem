package br.com.fotocastro.ui;

import br.com.fotocastro.infra.VendaDaoH2;
import br.com.fotocastro.utils.FxmlLoaderUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.time.LocalDate;

public class HomeController {

    @FXML private Label lblStatusCaixa;
    @FXML private Label lblStatusDb;
    @FXML private Label lblBackup;

    @FXML private Button btnAbrirCaixa;
    @FXML private Button btnFecharCaixa;

    private final VendaDaoH2 vendaDao = new VendaDaoH2();

    @FXML
    private void initialize() {
        atualizarStatus();
    }

    private void atualizarStatus() {
        boolean fechadoHoje = vendaDao.caixaJaFechado(LocalDate.now());

        if (fechadoHoje) {
            lblStatusCaixa.setText("FECHADO");
            lblStatusCaixa.getStyleClass().setAll("status-closed");
            btnAbrirCaixa.setDisable(true);
            btnFecharCaixa.setDisable(true);
        } else {
            lblStatusCaixa.setText("ABERTO");
            lblStatusCaixa.getStyleClass().setAll("status-open");
            btnAbrirCaixa.setDisable(false);
            btnFecharCaixa.setDisable(false);
        }

        lblStatusDb.setText("OK");
        lblBackup.setText("—");
    }

    @FXML
    public void abrirCaixa() {
        try {
            Node caixa = FXMLLoader.load(getClass().getResource("/ui/CaixaView.fxml"));
            MainNavigation.getInstance().setView(caixa);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erro ao abrir a tela do Caixa");
        }
    }

    @FXML
    private void abrirDashboard() {
        MainNavigation.getInstance().goTo("DASHBOARD");
    }

    @FXML
    private void onEstoque() {
        try {
            Node estoque = FxmlLoaderUtil.load("/ui/EstoqueView.fxml");
            MainNavigation.getInstance().setView(estoque);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void fecharCaixa() {
        MainNavigation.getInstance().goTo("CAIXA_FECHAMENTO");
    }
}