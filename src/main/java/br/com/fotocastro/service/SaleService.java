package br.com.fotocastro.service;

import br.com.fotocastro.infra.VendaDaoH2;
import br.com.fotocastro.model.PaymentMethod;
import br.com.fotocastro.model.Venda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Serviço para gerenciamento de vendas.
 * Contém a lógica de negócio, cálculo de taxas e validações.
 */
public class SaleService {
    
    private static final Logger logger = Logger.getLogger(SaleService.class.getName());
    
    // Taxas padrão (podem ser configuráveis depois)
    private static final double TAXA_CARTAO_PERCENT = 0.0533333333; // ~5.33%
    private static final double TAXA_PIX_PERCENT = 0.00;
    private static final double TAXA_DINHEIRO_PERCENT = 0.00;
    
    private final VendaDaoH2 vendaDao;
    
    public SaleService() {
        this.vendaDao = new VendaDaoH2();
    }
    
    // Construtor para injeção de dependência (testes)
    public SaleService(VendaDaoH2 vendaDao) {
        this.vendaDao = vendaDao;
    }
    
    // ========== OPERAÇÕES CRUD ==========
    
    /**
     * Registra uma nova venda
     */
    public Long registrarVenda(String descricao, double valorBruto, PaymentMethod metodo) {
        // Validações
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da venda é obrigatória");
        }
        
        if (valorBruto <= 0) {
            throw new IllegalArgumentException("Valor bruto deve ser maior que zero");
        }
        
        if (metodo == null) {
            throw new IllegalArgumentException("Método de pagamento é obrigatório");
        }
        
        // Calcula taxa e valor líquido
        double taxa = calcularTaxa(valorBruto, metodo);
        double valorLiquido = Math.max(0.0, valorBruto - taxa);
        
        // Cria venda
        Venda venda = new Venda();
        venda.setDataHora(LocalDateTime.now());
        venda.setDescricao(descricao.trim());
        venda.setValorBruto(round2(valorBruto));
        venda.setMetodo(metodo);
        venda.setTaxa(round2(taxa));
        venda.setValorLiquido(round2(valorLiquido));
        
        logger.info(String.format(
            "Registrando venda: %s - R$ %.2f (%s) - Taxa: R$ %.2f - Líquido: R$ %.2f",
            descricao, valorBruto, metodo, taxa, valorLiquido
        ));
        
        return vendaDao.inserir(venda);
    }
    
    /**
     * Atualiza uma venda existente
     */
    public void atualizarVenda(Long id, String descricao, double valorBruto, PaymentMethod metodo) {
        if (id == null) {
            throw new IllegalArgumentException("ID da venda não pode ser nulo");
        }
        
        // Verifica se a venda existe
        Venda vendaExistente = vendaDao.buscarPorId(id).orElseThrow(
            () -> new IllegalArgumentException("Venda não encontrada: " + id)
        );
        
        // Recalcula taxa e valor líquido
        double taxa = calcularTaxa(valorBruto, metodo);
        double valorLiquido = Math.max(0.0, valorBruto - taxa);
        
        // Atualiza dados
        vendaExistente.setDescricao(descricao.trim());
        vendaExistente.setValorBruto(round2(valorBruto));
        vendaExistente.setMetodo(metodo);
        vendaExistente.setTaxa(round2(taxa));
        vendaExistente.setValorLiquido(round2(valorLiquido));
        
        vendaDao.atualizar(vendaExistente);
        logger.info("Venda atualizada: ID=" + id);
    }
    
    /**
     * Remove uma venda
     */
    public void removerVenda(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        // Verifica se existe antes de remover
        vendaDao.buscarPorId(id).orElseThrow(
            () -> new IllegalArgumentException("Venda não encontrada: " + id)
        );
        
        logger.warning("Removendo venda: ID=" + id);
        vendaDao.remover(id);
    }
    
    /**
     * Busca venda por ID
     */
    public Optional<Venda> buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        return vendaDao.buscarPorId(id);
    }
    
    // ========== CONSULTAS ==========
    
    /**
     * Lista vendas recentes (limitado)
     */
    public List<Venda> listarRecentes(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("Limite deve ser maior que zero");
        }
        return vendaDao.listarRecentes(limite);
    }
    
    /**
     * Lista todas as vendas
     */
    public List<Venda> listarTodas() {
        return vendaDao.listarTodas();
    }
    
    /**
     * Lista vendas de hoje
     */
    public List<Venda> listarVendasHoje() {
        return vendaDao.listarHoje();
    }
    
    /**
     * Lista vendas por período
     */
    public List<Venda> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Datas não podem ser nulas");
        }
        
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Data inicial deve ser anterior à data final");
        }
        
        return vendaDao.listarPorPeriodo(inicio, fim);
    }
    
    // ========== CÁLCULOS E TAXAS ==========
    
    /**
     * Calcula taxa baseada no método de pagamento
     */
    public double calcularTaxa(double valor, PaymentMethod metodo) {
        if (valor < 0) {
            throw new IllegalArgumentException("Valor não pode ser negativo");
        }
        
        if (metodo == null) {
            throw new IllegalArgumentException("Método de pagamento não pode ser nulo");
        }
        
        double percentual = switch (metodo) {
            case CARTAO -> TAXA_CARTAO_PERCENT;
            case PIX -> TAXA_PIX_PERCENT;
            case DINHEIRO -> TAXA_DINHEIRO_PERCENT;
        };
        
        return round2(valor * percentual);
    }
    
    /**
     * Calcula valor líquido
     */
    public double calcularValorLiquido(double valorBruto, PaymentMethod metodo) {
        double taxa = calcularTaxa(valorBruto, metodo);
        return round2(Math.max(0.0, valorBruto - taxa));
    }
    
    /**
     * Retorna percentual de taxa do método
     */
    public double obterPercentualTaxa(PaymentMethod metodo) {
        return switch (metodo) {
            case CARTAO -> TAXA_CARTAO_PERCENT * 100;
            case PIX -> TAXA_PIX_PERCENT * 100;
            case DINHEIRO -> TAXA_DINHEIRO_PERCENT * 100;
        };
    }
    
    // ========== ESTATÍSTICAS ==========
    
    /**
     * Gera resumo financeiro geral
     */
    public ResumoFinanceiro gerarResumoGeral() {
        double totalBruto = vendaDao.somaBruto();
        double totalTaxas = vendaDao.somaTaxas();
        double totalLiquido = vendaDao.somaLiquido();
        
        double liquidoDinheiro = vendaDao.somaPorMetodo(PaymentMethod.DINHEIRO);
        double liquidoCartao = vendaDao.somaPorMetodo(PaymentMethod.CARTAO);
        double liquidoPix = vendaDao.somaPorMetodo(PaymentMethod.PIX);
        
        return new ResumoFinanceiro(
            totalBruto, totalTaxas, totalLiquido,
            liquidoDinheiro, liquidoCartao, liquidoPix
        );
    }
    
    /**
     * Gera resumo de vendas de hoje
     */
    public ResumoVendasHoje gerarResumoHoje() {
        int quantidade = vendaDao.contarVendasHoje();
        double liquidoHoje = vendaDao.somaLiquidoHoje();
        
        double liquidoDinheiro = vendaDao.somaPorMetodoHoje(PaymentMethod.DINHEIRO);
        double liquidoCartao = vendaDao.somaPorMetodoHoje(PaymentMethod.CARTAO);
        double liquidoPix = vendaDao.somaPorMetodoHoje(PaymentMethod.PIX);
        
        return new ResumoVendasHoje(
            quantidade, liquidoHoje,
            liquidoDinheiro, liquidoCartao, liquidoPix
        );
    }
    
    /**
     * Calcula total de vendas por período
     */
    public double calcularTotalPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Datas não podem ser nulas");
        }
        return vendaDao.somaPorPeriodo(inicio, fim);
    }
    
    // ========== UTILITÁRIOS ==========
    
    /**
     * Arredonda para 2 casas decimais
     */
    private double round2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
    
    /**
     * Formata valor para exibição
     */
    public String formatarMoeda(double valor) {
        return String.format("R$ %.2f", valor);
    }
    
    // ========== CLASSES DE RESUMO ==========
    
    /**
     * DTO para resumo financeiro geral
     */
    public static class ResumoFinanceiro {
        private final double totalBruto;
        private final double totalTaxas;
        private final double totalLiquido;
        private final double liquidoDinheiro;
        private final double liquidoCartao;
        private final double liquidoPix;
        
        public ResumoFinanceiro(double totalBruto, double totalTaxas, double totalLiquido,
                                double liquidoDinheiro, double liquidoCartao, double liquidoPix) {
            this.totalBruto = totalBruto;
            this.totalTaxas = totalTaxas;
            this.totalLiquido = totalLiquido;
            this.liquidoDinheiro = liquidoDinheiro;
            this.liquidoCartao = liquidoCartao;
            this.liquidoPix = liquidoPix;
        }
        
        public double getTotalBruto() { return totalBruto; }
        public double getTotalTaxas() { return totalTaxas; }
        public double getTotalLiquido() { return totalLiquido; }
        public double getLiquidoDinheiro() { return liquidoDinheiro; }
        public double getLiquidoCartao() { return liquidoCartao; }
        public double getLiquidoPix() { return liquidoPix; }
        
        @Override
        public String toString() {
            return String.format(
                "Resumo Financeiro:%n" +
                "  Total Bruto: R$ %.2f%n" +
                "  Total Taxas: R$ %.2f%n" +
                "  Total Líquido: R$ %.2f%n" +
                "  Dinheiro: R$ %.2f%n" +
                "  Cartão: R$ %.2f%n" +
                "  PIX: R$ %.2f",
                totalBruto, totalTaxas, totalLiquido,
                liquidoDinheiro, liquidoCartao, liquidoPix
            );
        }
    }
    
    /**
     * DTO para resumo de vendas de hoje
     */
    public static class ResumoVendasHoje {
        private final int quantidade;
        private final double totalLiquido;
        private final double liquidoDinheiro;
        private final double liquidoCartao;
        private final double liquidoPix;
        
        public ResumoVendasHoje(int quantidade, double totalLiquido,
                                double liquidoDinheiro, double liquidoCartao, double liquidoPix) {
            this.quantidade = quantidade;
            this.totalLiquido = totalLiquido;
            this.liquidoDinheiro = liquidoDinheiro;
            this.liquidoCartao = liquidoCartao;
            this.liquidoPix = liquidoPix;
        }
        
        public int getQuantidade() { return quantidade; }
        public double getTotalLiquido() { return totalLiquido; }
        public double getLiquidoDinheiro() { return liquidoDinheiro; }
        public double getLiquidoCartao() { return liquidoCartao; }
        public double getLiquidoPix() { return liquidoPix; }
        
        @Override
        public String toString() {
            return String.format(
                "Vendas de Hoje:%n" +
                "  Quantidade: %d%n" +
                "  Total Líquido: R$ %.2f%n" +
                "  Dinheiro: R$ %.2f | Cartão: R$ %.2f | PIX: R$ %.2f",
                quantidade, totalLiquido,
                liquidoDinheiro, liquidoCartao, liquidoPix
            );
        }
    }
}