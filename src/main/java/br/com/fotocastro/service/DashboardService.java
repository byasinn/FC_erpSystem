package br.com.fotocastro.service;

import br.com.fotocastro.model.ItemEstoque;
import br.com.fotocastro.model.Venda;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Serviço para geração de dashboards e KPIs.
 * Agrega dados de diferentes serviços.
 */
public class DashboardService {
    
    private static final Logger logger = Logger.getLogger(DashboardService.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    private final SaleService saleService;
    private final InventoryService inventoryService;
    
    public DashboardService() {
        this.saleService = new SaleService();
        this.inventoryService = new InventoryService();
    }
    
    // Construtor para injeção de dependência (testes)
    public DashboardService(SaleService saleService, InventoryService inventoryService) {
        this.saleService = saleService;
        this.inventoryService = inventoryService;
    }
    
    // ========== KPIs PRINCIPAIS ==========
    
    /**
     * Gera KPIs da home (tela inicial)
     */
    public HomeKPIs gerarKPIsHome() {
        // Vendas de hoje
        SaleService.ResumoVendasHoje vendasHoje = saleService.gerarResumoHoje();
        
        // Resumo financeiro total
        SaleService.ResumoFinanceiro resumoGeral = saleService.gerarResumoGeral();
        
        // Estoque crítico
        int itensCriticos = inventoryService.contarItensCriticos();
        List<ItemEstoque> itensBaixos = inventoryService.listarEstoqueBaixo();
        
        // Últimas vendas
        List<Venda> ultimasVendas = saleService.listarRecentes(10);
        
        return new HomeKPIs(
            vendasHoje.getQuantidade(),
            vendasHoje.getTotalLiquido(),
            resumoGeral.getTotalLiquido(),
            itensCriticos,
            itensBaixos,
            ultimasVendas
        );
    }
    
    /**
     * Gera resumo completo do caixa
     */
    public ResumoCaixa gerarResumoCaixa() {
        SaleService.ResumoFinanceiro resumo = saleService.gerarResumoGeral();
        
        return new ResumoCaixa(
            resumo.getTotalBruto(),
            resumo.getTotalTaxas(),
            resumo.getTotalLiquido(),
            resumo.getLiquidoDinheiro(),
            resumo.getLiquidoCartao(),
            resumo.getLiquidoPix()
        );
    }
    
    /**
     * Gera resumo do estoque
     */
    public ResumoEstoque gerarResumoEstoque() {
        InventoryService.RelatorioEstoque relatorio = inventoryService.gerarRelatorio();
        
        return new ResumoEstoque(
            relatorio.getTotalItens(),
            relatorio.getTotalQuantidade(),
            relatorio.getValorTotal(),
            relatorio.getItensCriticos(),
            relatorio.getItensBaixos()
        );
    }
    
    // ========== RELATÓRIOS POR PERÍODO ==========
    
    /**
     * Gera relatório de vendas por período
     */
    public RelatorioVendasPeriodo gerarRelatorioVendas(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Datas não podem ser nulas");
        }
        
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Data inicial deve ser anterior à final");
        }
        
        List<Venda> vendas = saleService.listarPorPeriodo(inicio, fim);
        
        int quantidade = vendas.size();
        double totalBruto = vendas.stream().mapToDouble(Venda::getValorBruto).sum();
        double totalTaxas = vendas.stream().mapToDouble(Venda::getTaxa).sum();
        double totalLiquido = vendas.stream().mapToDouble(Venda::getValorLiquido).sum();
        
        // Média por dia
        long diasPeriodo = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        double mediaDiaria = diasPeriodo > 0 ? totalLiquido / diasPeriodo : 0;
        
        return new RelatorioVendasPeriodo(
            inicio, fim,
            quantidade, totalBruto, totalTaxas, totalLiquido,
            mediaDiaria, vendas
        );
    }
    
    /**
     * Gera relatório de vendas do mês atual
     */
    public RelatorioVendasPeriodo gerarRelatorioMesAtual() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        
        return gerarRelatorioVendas(inicioMes, fimMes);
    }
    
    /**
     * Gera relatório de vendas do mês anterior
     */
    public RelatorioVendasPeriodo gerarRelatorioMesAnterior() {
        LocalDate mesAnterior = LocalDate.now().minusMonths(1);
        LocalDate inicio = mesAnterior.withDayOfMonth(1);
        LocalDate fim = mesAnterior.withDayOfMonth(mesAnterior.lengthOfMonth());
        
        return gerarRelatorioVendas(inicio, fim);
    }
    
    // ========== ANÁLISES ==========
    
    /**
     * Compara vendas de dois períodos
     */
    public ComparacaoPeriodos compararPeriodos(LocalDate inicio1, LocalDate fim1,
                                                 LocalDate inicio2, LocalDate fim2) {
        double totalPeriodo1 = saleService.calcularTotalPeriodo(inicio1, fim1);
        double totalPeriodo2 = saleService.calcularTotalPeriodo(inicio2, fim2);
        
        double diferenca = totalPeriodo2 - totalPeriodo1;
        double percentual = totalPeriodo1 > 0 ? (diferenca / totalPeriodo1) * 100 : 0;
        
        return new ComparacaoPeriodos(
            inicio1, fim1, totalPeriodo1,
            inicio2, fim2, totalPeriodo2,
            diferenca, percentual
        );
    }
    
    /**
     * Identifica produtos com movimento recente
     */
    public List<ItemEstoque> identificarProdutosParados() {
        // Por enquanto retorna itens com estoque alto
        // Futuramente pode cruzar com histórico de vendas
        return inventoryService.listarTodos().stream()
            .filter(item -> item.getQuantidade() > 50)
            .toList();
    }
    
    // ========== ALERTAS ==========
    
    /**
     * Verifica se há alertas importantes
     */
    public Alertas verificarAlertas() {
        int itensCriticos = inventoryService.contarItensCriticos();
        
        boolean estoqueEmAlerta = itensCriticos > 0;
        boolean semVendasHoje = saleService.gerarResumoHoje().getQuantidade() == 0;
        
        return new Alertas(estoqueEmAlerta, itensCriticos, semVendasHoje);
    }
    
    // ========== CLASSES DE RETORNO ==========
    
    /**
     * KPIs para a tela inicial
     */
    public static class HomeKPIs {
        private final int vendasHoje;
        private final double liquidoHoje;
        private final double liquidoAcumulado;
        private final int estoqueCritico;
        private final List<ItemEstoque> itensBaixos;
        private final List<Venda> ultimasVendas;
        
        public HomeKPIs(int vendasHoje, double liquidoHoje, double liquidoAcumulado,
                        int estoqueCritico, List<ItemEstoque> itensBaixos, List<Venda> ultimasVendas) {
            this.vendasHoje = vendasHoje;
            this.liquidoHoje = liquidoHoje;
            this.liquidoAcumulado = liquidoAcumulado;
            this.estoqueCritico = estoqueCritico;
            this.itensBaixos = itensBaixos;
            this.ultimasVendas = ultimasVendas;
        }
        
        public int getVendasHoje() { return vendasHoje; }
        public double getLiquidoHoje() { return liquidoHoje; }
        public double getLiquidoAcumulado() { return liquidoAcumulado; }
        public int getEstoqueCritico() { return estoqueCritico; }
        public List<ItemEstoque> getItensBaixos() { return itensBaixos; }
        public List<Venda> getUltimasVendas() { return ultimasVendas; }
    }
    
    /**
     * Resumo do caixa
     */
    public static class ResumoCaixa {
        private final double totalBruto;
        private final double totalTaxas;
        private final double totalLiquido;
        private final double liquidoDinheiro;
        private final double liquidoCartao;
        private final double liquidoPix;
        
        public ResumoCaixa(double totalBruto, double totalTaxas, double totalLiquido,
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
    }
    
    /**
     * Resumo do estoque
     */
    public static class ResumoEstoque {
        private final int totalItens;
        private final int totalQuantidade;
        private final double valorTotal;
        private final int itensCriticos;
        private final List<ItemEstoque> itensBaixos;
        
        public ResumoEstoque(int totalItens, int totalQuantidade, double valorTotal,
                             int itensCriticos, List<ItemEstoque> itensBaixos) {
            this.totalItens = totalItens;
            this.totalQuantidade = totalQuantidade;
            this.valorTotal = valorTotal;
            this.itensCriticos = itensCriticos;
            this.itensBaixos = itensBaixos;
        }
        
        public int getTotalItens() { return totalItens; }
        public int getTotalQuantidade() { return totalQuantidade; }
        public double getValorTotal() { return valorTotal; }
        public int getItensCriticos() { return itensCriticos; }
        public List<ItemEstoque> getItensBaixos() { return itensBaixos; }
    }
    
    /**
     * Relatório de vendas por período
     */
    public static class RelatorioVendasPeriodo {
        private final LocalDate dataInicio;
        private final LocalDate dataFim;
        private final int quantidade;
        private final double totalBruto;
        private final double totalTaxas;
        private final double totalLiquido;
        private final double mediaDiaria;
        private final List<Venda> vendas;
        
        public RelatorioVendasPeriodo(LocalDate dataInicio, LocalDate dataFim,
                                      int quantidade, double totalBruto, double totalTaxas,
                                      double totalLiquido, double mediaDiaria, List<Venda> vendas) {
            this.dataInicio = dataInicio;
            this.dataFim = dataFim;
            this.quantidade = quantidade;
            this.totalBruto = totalBruto;
            this.totalTaxas = totalTaxas;
            this.totalLiquido = totalLiquido;
            this.mediaDiaria = mediaDiaria;
            this.vendas = vendas;
        }
        
        public LocalDate getDataInicio() { return dataInicio; }
        public LocalDate getDataFim() { return dataFim; }
        public int getQuantidade() { return quantidade; }
        public double getTotalBruto() { return totalBruto; }
        public double getTotalTaxas() { return totalTaxas; }
        public double getTotalLiquido() { return totalLiquido; }
        public double getMediaDiaria() { return mediaDiaria; }
        public List<Venda> getVendas() { return vendas; }
        
        @Override
        public String toString() {
            return String.format(
                "Relatório de Vendas%n" +
                "Período: %s a %s%n" +
                "Quantidade: %d vendas%n" +
                "Total Bruto: R$ %.2f%n" +
                "Total Taxas: R$ %.2f%n" +
                "Total Líquido: R$ %.2f%n" +
                "Média Diária: R$ %.2f",
                DATE_FORMATTER.format(dataInicio), DATE_FORMATTER.format(dataFim),
                quantidade, totalBruto, totalTaxas, totalLiquido, mediaDiaria
            );
        }
    }
    
    /**
     * Comparação entre dois períodos
     */
    public static class ComparacaoPeriodos {
        private final LocalDate inicio1, fim1;
        private final double total1;
        private final LocalDate inicio2, fim2;
        private final double total2;
        private final double diferenca;
        private final double percentual;
        
        public ComparacaoPeriodos(LocalDate inicio1, LocalDate fim1, double total1,
                                  LocalDate inicio2, LocalDate fim2, double total2,
                                  double diferenca, double percentual) {
            this.inicio1 = inicio1;
            this.fim1 = fim1;
            this.total1 = total1;
            this.inicio2 = inicio2;
            this.fim2 = fim2;
            this.total2 = total2;
            this.diferenca = diferenca;
            this.percentual = percentual;
        }
        
        public LocalDate getInicio1() { return inicio1; }
        public LocalDate getFim1() { return fim1; }
        public double getTotal1() { return total1; }
        public LocalDate getInicio2() { return inicio2; }
        public LocalDate getFim2() { return fim2; }
        public double getTotal2() { return total2; }
        public double getDiferenca() { return diferenca; }
        public double getPercentual() { return percentual; }
        
        public boolean isMelhorou() { return diferenca > 0; }
        public boolean isPiorou() { return diferenca < 0; }
    }
    
    /**
     * Alertas do sistema
     */
    public static class Alertas {
        private final boolean estoqueEmAlerta;
        private final int itensCriticos;
        private final boolean semVendasHoje;
        
        public Alertas(boolean estoqueEmAlerta, int itensCriticos, boolean semVendasHoje) {
            this.estoqueEmAlerta = estoqueEmAlerta;
            this.itensCriticos = itensCriticos;
            this.semVendasHoje = semVendasHoje;
        }
        
        public boolean isEstoqueEmAlerta() { return estoqueEmAlerta; }
        public int getItensCriticos() { return itensCriticos; }
        public boolean isSemVendasHoje() { return semVendasHoje; }
        public boolean temAlgumAlerta() { return estoqueEmAlerta || semVendasHoje; }
    }
}