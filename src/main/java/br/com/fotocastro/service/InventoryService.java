package br.com.fotocastro.service;

import br.com.fotocastro.infra.EstoqueDaoH2;
import br.com.fotocastro.model.ItemEstoque;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Serviço para gerenciamento de estoque.
 * Contém a lógica de negócio e validações.
 */
public class InventoryService {
    
    private static final Logger logger = Logger.getLogger(InventoryService.class.getName());
    private static final int QUANTIDADE_MINIMA_ALERTA = 10;
    
    private final EstoqueDaoH2 estoqueDao;
    
    public InventoryService() {
        this.estoqueDao = new EstoqueDaoH2();
    }
    
    // Construtor para injeção de dependência (testes)
    public InventoryService(EstoqueDaoH2 estoqueDao) {
        this.estoqueDao = estoqueDao;
    }
    
    // ========== OPERAÇÕES CRUD ==========
    
    /**
     * Lista todos os itens de estoque
     */
    public List<ItemEstoque> listarTodos() {
        return estoqueDao.listarTodos();
    }
    
    /**
     * Busca um item por ID
     */
    public Optional<ItemEstoque> buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        return estoqueDao.buscarPorId(id);
    }
    
    /**
     * Adiciona um novo item ao estoque
     */
    public Long adicionarItem(ItemEstoque item) {
        validateItemBasico(item);
        
        logger.info(String.format("Adicionando novo item: %s (Qtd: %d, Custo: R$ %.2f)",
            item.getNome(), item.getQuantidade(), item.getCustoTotal()));
        
        return estoqueDao.adicionar(item);
    }
    
    /**
     * Atualiza informações cadastrais (nome, cor, tamanho)
     */
    public void atualizarCadastro(ItemEstoque item) {
        validateItemBasico(item);
        
        if (item.getId() == null) {
            throw new IllegalArgumentException("ID do item não pode ser nulo para atualização");
        }
        
        // Verifica se o item existe
        buscarPorId(item.getId()).orElseThrow(
            () -> new IllegalArgumentException("Item não encontrado: " + item.getId())
        );
        
        estoqueDao.atualizarCadastro(item);
        logger.info("Cadastro atualizado: " + item.getNome());
    }
    
    /**
     * Remove um item do estoque
     * Verifica se há quantidade antes de remover
     */
    public void removerItem(Long id) {
        ItemEstoque item = buscarPorId(id).orElseThrow(
            () -> new IllegalArgumentException("Item não encontrado: " + id)
        );
        
        if (item.getQuantidade() > 0) {
            logger.warning(String.format(
                "ATENÇÃO: Removendo item com quantidade em estoque: %s (Qtd: %d)",
                item.getNome(), item.getQuantidade()
            ));
        }
        
        estoqueDao.remover(id);
        logger.info("Item removido: " + item.getNome());
    }
    
    // ========== MOVIMENTAÇÕES DE ESTOQUE ==========
    
    /**
     * Registra entrada de lote (compra/recebimento)
     */
    public void registrarEntradaLote(Long itemId, int quantidade, double custoTotalLote) {
        // Validações
        if (itemId == null) {
            throw new IllegalArgumentException("ID do item não pode ser nulo");
        }
        
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        if (custoTotalLote < 0) {
            throw new IllegalArgumentException("Custo total não pode ser negativo");
        }
        
        // Verifica se o item existe
        ItemEstoque item = buscarPorId(itemId).orElseThrow(
            () -> new IllegalArgumentException("Item não encontrado: " + itemId)
        );
        
        double custoUnitario = custoTotalLote / quantidade;
        
        logger.info(String.format(
            "Entrada de lote: %s - Qtd: %d, Custo unit: R$ %.2f, Custo total: R$ %.2f",
            item.getNome(), quantidade, custoUnitario, custoTotalLote
        ));
        
        estoqueDao.entradaLote(itemId, quantidade, custoTotalLote);
    }
    
    /**
     * Registra saída/consumo de estoque
     */
    public void registrarSaida(Long itemId, int quantidade, String motivo) {
        // Validações
        if (itemId == null) {
            throw new IllegalArgumentException("ID do item não pode ser nulo");
        }
        
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        // Verifica se o item existe e tem quantidade suficiente
        ItemEstoque item = buscarPorId(itemId).orElseThrow(
            () -> new IllegalArgumentException("Item não encontrado: " + itemId)
        );
        
        if (item.getQuantidade() < quantidade) {
            throw new IllegalArgumentException(String.format(
                "Quantidade insuficiente em estoque. Disponível: %d, Solicitado: %d",
                item.getQuantidade(), quantidade
            ));
        }
        
        String motivoLog = motivo != null && !motivo.isEmpty() ? motivo : "Não especificado";
        
        logger.info(String.format(
            "Saída de estoque: %s - Qtd: %d, Motivo: %s",
            item.getNome(), quantidade, motivoLog
        ));
        
        estoqueDao.saidaConsumo(itemId, quantidade);
    }
    
    /**
     * Ajuste manual de quantidade e custo
     * Útil para correções de inventário
     */
    public void ajustarEstoque(Long itemId, int novaQuantidade, double novoCustoTotal, String motivo) {
        if (itemId == null) {
            throw new IllegalArgumentException("ID do item não pode ser nulo");
        }
        
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        
        if (novoCustoTotal < 0) {
            throw new IllegalArgumentException("Custo total não pode ser negativo");
        }
        
        ItemEstoque item = buscarPorId(itemId).orElseThrow(
            () -> new IllegalArgumentException("Item não encontrado: " + itemId)
        );
        
        logger.warning(String.format(
            "AJUSTE DE ESTOQUE: %s - De: Qtd=%d/Custo=R$%.2f Para: Qtd=%d/Custo=R$%.2f - Motivo: %s",
            item.getNome(), item.getQuantidade(), item.getCustoTotal(),
            novaQuantidade, novoCustoTotal, motivo != null ? motivo : "Não informado"
        ));
        
        estoqueDao.atualizarQuantidadeECusto(itemId, novaQuantidade, novoCustoTotal);
    }
    
    // ========== CONSULTAS E RELATÓRIOS ==========
    
    /**
     * Lista itens com estoque baixo (abaixo do mínimo)
     */
    public List<ItemEstoque> listarEstoqueBaixo() {
        return estoqueDao.listarEstoqueBaixo(QUANTIDADE_MINIMA_ALERTA);
    }
    
    /**
     * Conta quantos itens estão com estoque crítico
     */
    public int contarItensCriticos() {
        return listarEstoqueBaixo().size();
    }
    
    /**
     * Verifica se um item específico está com estoque baixo
     */
    public boolean isEstoqueBaixo(Long itemId) {
        ItemEstoque item = buscarPorId(itemId).orElse(null);
        return item != null && item.getQuantidade() <= QUANTIDADE_MINIMA_ALERTA;
    }
    
    /**
     * Calcula valor total investido em estoque
     */
    public double calcularValorTotalEstoque() {
        return estoqueDao.calcularValorTotal();
    }
    
    /**
     * Conta total de itens cadastrados
     */
    public int contarItens() {
        return estoqueDao.contarItens();
    }
    
    /**
     * Gera relatório resumido do estoque
     */
    public RelatorioEstoque gerarRelatorio() {
        List<ItemEstoque> todos = listarTodos();
        List<ItemEstoque> criticos = listarEstoqueBaixo();
        
        int totalItens = todos.size();
        int totalQuantidade = todos.stream().mapToInt(ItemEstoque::getQuantidade).sum();
        double valorTotal = calcularValorTotalEstoque();
        int itensCriticos = criticos.size();
        
        return new RelatorioEstoque(totalItens, totalQuantidade, valorTotal, itensCriticos, criticos);
    }
    
    // ========== VALIDAÇÕES PRIVADAS ==========
    
    private void validateItemBasico(ItemEstoque item) {
        if (item == null) {
            throw new IllegalArgumentException("Item não pode ser nulo");
        }
        
        if (item.getNome() == null || item.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do item é obrigatório");
        }
        
        if (item.getQuantidade() < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        
        if (item.getCustoTotal() < 0) {
            throw new IllegalArgumentException("Custo total não pode ser negativo");
        }
    }
    
    // ========== CLASSE DE RELATÓRIO ==========
    
    /**
     * DTO para relatório de estoque
     */
    public static class RelatorioEstoque {
        private final int totalItens;
        private final int totalQuantidade;
        private final double valorTotal;
        private final int itensCriticos;
        private final List<ItemEstoque> itensBaixos;
        
        public RelatorioEstoque(int totalItens, int totalQuantidade, double valorTotal,
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
        
        @Override
        public String toString() {
            return String.format(
                "Relatório de Estoque:%n" +
                "  Total de Itens: %d%n" +
                "  Quantidade Total: %d unidades%n" +
                "  Valor Total: R$ %.2f%n" +
                "  Itens Críticos: %d",
                totalItens, totalQuantidade, valorTotal, itensCriticos
            );
        }
    }
}