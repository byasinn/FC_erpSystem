package br.com.fotocastro.model;

import br.com.fotocastro.template.TemplateIcon;

/**
 * Template de venda com suporte a ícones, tags e vinculação a estoque.
 */
public class TemplateVenda {
    
    private Long id;
    private String nome;
    private double preco;
    private String tamanho;              // ex: "10x15", "A4"
    private TemplateIcon icone;          // ícone visual
    private String tag;                  // categoria/tag
    private Long estoqueItemId;          // FK para item de estoque
    private int quantidadeUso;           // quantas unidades de estoque consome
    private boolean ativo;
    
    // Campos transientes (não persistidos, apenas para exibição)
    private String estoqueItemNome;      // nome do item de estoque
    private int estoqueDisponivel;       // quantidade disponível
    
    public TemplateVenda() {
        this.quantidadeUso = 1;
        this.ativo = true;
        this.icone = TemplateIcon.PHOTO; // padrão
        this.estoqueDisponivel = 0;
    }
    
    public TemplateVenda(String nome, double preco) {
        this();
        this.nome = nome;
        this.preco = preco;
    }
    
    public TemplateVenda(String nome, double preco, String tamanho, TemplateIcon icone) {
        this(nome, preco);
        this.tamanho = tamanho;
        this.icone = icone;
    }
    
    // ========== Getters e Setters ==========
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public double getPreco() {
        return preco;
    }
    
    public void setPreco(double preco) {
        this.preco = preco;
    }
    
    public String getTamanho() {
        return tamanho;
    }
    
    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }
    
    public TemplateIcon getIcone() {
        return icone != null ? icone : TemplateIcon.PHOTO;
    }
    
    public void setIcone(TemplateIcon icone) {
        this.icone = icone;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public Long getEstoqueItemId() {
        return estoqueItemId;
    }
    
    public void setEstoqueItemId(Long estoqueItemId) {
        this.estoqueItemId = estoqueItemId;
    }
    
    public int getQuantidadeUso() {
        return quantidadeUso;
    }
    
    public void setQuantidadeUso(int quantidadeUso) {
        this.quantidadeUso = quantidadeUso;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public String getEstoqueItemNome() {
        return estoqueItemNome;
    }
    
    public void setEstoqueItemNome(String estoqueItemNome) {
        this.estoqueItemNome = estoqueItemNome;
    }
    
    public int getEstoqueDisponivel() {
        return estoqueDisponivel;
    }
    
    public void setEstoqueDisponivel(int estoqueDisponivel) {
        this.estoqueDisponivel = estoqueDisponivel;
    }
    
    // ========== Métodos Utilitários ==========
    
    /**
     * Retorna descrição completa do template
     */
    public String getDescricaoCompleta() {
        StringBuilder sb = new StringBuilder(nome);
        if (tamanho != null && !tamanho.isEmpty()) {
            sb.append(" (").append(tamanho).append(")");
        }
        return sb.toString();
    }
    
    /**
     * Verifica se o template tem estoque vinculado
     */
    public boolean temEstoqueVinculado() {
        return estoqueItemId != null;
    }
    
    /**
     * Verifica se há estoque suficiente para venda
     */
    public boolean temEstoqueSuficiente(int quantidadeVenda) {
        if (!temEstoqueVinculado()) {
            return true; // se não tem estoque vinculado, sempre ok
        }
        int necessario = quantidadeUso * quantidadeVenda;
        return estoqueDisponivel >= necessario;
    }
    
    /**
     * Calcula quanto de estoque será consumido
     */
    public int calcularConsumoEstoque(int quantidadeVenda) {
        return quantidadeUso * quantidadeVenda;
    }
    
    @Override
    public String toString() {
        return String.format("%s - R$ %.2f", getDescricaoCompleta(), preco);
    }
}