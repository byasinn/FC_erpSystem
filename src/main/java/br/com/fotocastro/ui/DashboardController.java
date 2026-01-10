package br.com.fotocastro.ui;

import br.com.fotocastro.infra.VendaDaoH2;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

public class DashboardController {

    @FXML private LineChart<String, Number> chartVendasDiarias;
    @FXML private BarChart<String, Number> chartPorHora;
    @FXML private PieChart chartTopTemplates;

    private VendaDaoH2 vendaDao = new VendaDaoH2();

    @FXML
    private void initialize() {
        carregarTudo();

        VendaEventBus.vendaAtualizadaProperty().addListener((obs, oldV, newV) -> {
            atualizarDashboard();
        });
    }

    private void limparGraficos() {
        chartVendasDiarias.getData().clear();
        chartPorHora.getData().clear();
        chartTopTemplates.getData().clear();
    }

    private void atualizarDashboard() {
        limparGraficos();
        carregarTudo();
    }

    private void carregarTudo() {
        carregarVendasDiarias();
        carregarPorHora();
        carregarTopTemplates();
    }

        private void carregarVendasDiarias() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        vendaDao.vendasLiquidasPorDiaUltimos30().forEach((data, valor) ->
            series.getData().add(new XYChart.Data<>(data.toString(), valor)));
        chartVendasDiarias.getData().add(series);
    }

    private void carregarPorHora() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        vendaDao.vendasPorHora().forEach((hora, valor) ->
            series.getData().add(new XYChart.Data<>(hora + "h", valor)));
        chartPorHora.getData().add(series);
    }

    private void carregarTopTemplates() {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        vendaDao.topDescricoes(10).forEach(entry ->
            pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue())));
        chartTopTemplates.setData(pieData);
    }
}