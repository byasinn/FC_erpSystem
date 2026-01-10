package br.com.fotocastro.model;

public class ItemEstoque {
    private Long id;
    private String nome;
    private String cor;
    private String tamanho;
    private int quantidade;
    private double custoTotal;

    public ItemEstoque() {}

    public ItemEstoque(String nome, String cor, String tamanho, int quantidade, double custoTotal) {
        this.nome = nome;
        this.cor = cor;
        this.tamanho = tamanho;
        this.quantidade = quantidade;
        this.custoTotal = custoTotal;
    }

    public double getCustoUnitario() {
        return quantidade > 0 ? custoTotal / quantidade : 0.0;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getTamanho() { return tamanho; }
    public void setTamanho(String tamanho) { this.tamanho = tamanho; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getCustoTotal() { return custoTotal; }
    public void setCustoTotal(double custoTotal) { this.custoTotal = custoTotal; }
}
