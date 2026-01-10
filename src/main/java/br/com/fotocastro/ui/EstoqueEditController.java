package br.com.fotocastro.ui;

import br.com.fotocastro.model.ItemEstoque;
import javafx.fxml.FXML;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;

public class EstoqueEditController {

    @FXML private DialogPane dialogPane;
    @FXML private TextField txtNome;
    @FXML private TextField txtCor;
    @FXML private TextField txtTamanho;
    @FXML private TextField txtQuantidade;
    @FXML private TextField txtCustoTotal;

    private ItemEstoque item; // pode ser novo ou existente
    private boolean entradaDeLote = false; // quando true, soma qtd/custo ao existente

    public void setItem(ItemEstoque item, boolean entradaDeLote) {
        this.item = item;
        this.entradaDeLote = entradaDeLote;

        if (item != null && !entradaDeLote) {
            // edição cadastral ou edição total
            txtNome.setText(nvl(item.getNome()));
            txtCor.setText(nvl(item.getCor()));
            txtTamanho.setText(nvl(item.getTamanho()));
            txtQuantidade.setText(String.valueOf(item.getQuantidade()));
            txtCustoTotal.setText(String.format("%.2f", item.getCustoTotal()));
        } else {
            // novo item ou entrada de lote em item existente
            txtQuantidade.setText("0");
            txtCustoTotal.setText("0.00");
        }

        // Se for entrada de lote, bloqueia campos cadastrais e quantidade atual
        if (entradaDeLote && item != null) {
            txtNome.setText(item.getNome());
            txtCor.setText(nvl(item.getCor()));
            txtTamanho.setText(nvl(item.getTamanho()));
            txtNome.setEditable(false);
            txtCor.setEditable(false);
            txtTamanho.setEditable(false);
        }
    }

    public ItemEstoque getResultado() {
        // Validações mínimas
        final String nome = safe(txtNome.getText());
        if (nome.isBlank()) throw new IllegalArgumentException("Nome é obrigatório.");

        final String cor = safe(txtCor.getText());
        final String tamanho = safe(txtTamanho.getText());
        final int quantidade = parseInt(txtQuantidade.getText());
        final double custoTotal = parseDouble(txtCustoTotal.getText());

        if (entradaDeLote && item != null) {
            // retorna apenas um "delta" — o chamador decide aplicar como entrada
            return new ItemEstoque(nome, cor, tamanho, quantidade, custoTotal);
        }

        if (item == null) item = new ItemEstoque();
        item.setNome(nome);
        item.setCor(cor.isBlank() ? null : cor);
        item.setTamanho(tamanho.isBlank() ? null : tamanho);
        item.setQuantidade(Math.max(0, quantidade));
        item.setCustoTotal(Math.max(0.0, custoTotal));
        return item;
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".").trim()); } catch (Exception e) { return 0.0; }
    }
}
