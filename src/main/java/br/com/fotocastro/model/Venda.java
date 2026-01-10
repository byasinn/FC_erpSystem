package br.com.fotocastro.model;

import java.time.LocalDateTime;

public class Venda {
    private Long id;
    private LocalDateTime dataHora;
    private String descricao;
    private double valorBruto;
    private PaymentMethod metodo;
    private double taxa;
    private double valorLiquido;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getValorBruto() { return valorBruto; }
    public void setValorBruto(double valorBruto) { this.valorBruto = valorBruto; }

    public PaymentMethod getMetodo() { return metodo; }
    public void setMetodo(PaymentMethod metodo) { this.metodo = metodo; }

    public double getTaxa() { return taxa; }
    public void setTaxa(double taxa) { this.taxa = taxa; }

    public double getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(double valorLiquido) { this.valorLiquido = valorLiquido; }
}
