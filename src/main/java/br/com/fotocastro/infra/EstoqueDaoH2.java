package br.com.fotocastro.infra;

import br.com.fotocastro.model.ItemEstoque;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gerenciamento de estoque.
 * Versão refatorada usando DAOBase e connection pool.
 */
public class EstoqueDaoH2 extends DAOBase {

    /**
     * Lista todos os itens de estoque ordenados
     */
    public List<ItemEstoque> listarTodos() {
        String sql = "SELECT * FROM estoque ORDER BY nome, cor, tamanho";
        return executeQuery(sql, this::mapToItemEstoque);
    }

    /**
     * Busca um item por ID
     */
    public Optional<ItemEstoque> buscarPorId(Long id) {
        String sql = "SELECT * FROM estoque WHERE id = ?";
        return executeQuerySingle(sql, this::mapToItemEstoque, id);
    }

    /**
     * Adiciona um novo item ao estoque
     */
    public Long adicionar(ItemEstoque item) {
        validateItem(item);
        
        String sql = "INSERT INTO estoque (nome, cor, tamanho, quantidade, custo_total) VALUES (?, ?, ?, ?, ?)";
        
        return executeInsert(sql,
            item.getNome(),
            item.getCor(),
            item.getTamanho(),
            item.getQuantidade(),
            item.getCustoTotal()
        );
    }

    /**
     * Atualiza cadastro básico (nome, cor, tamanho)
     */
    public void atualizarCadastro(ItemEstoque item) {
        validateItem(item);
        
        if (item.getId() == null) {
            throw new IllegalArgumentException("ID do item não pode ser nulo");
        }
        
        String sql = "UPDATE estoque SET nome = ?, cor = ?, tamanho = ? WHERE id = ?";
        
        int affected = executeUpdate(sql,
            item.getNome(),
            item.getCor(),
            item.getTamanho(),
            item.getId()
        );
        
        if (affected == 0) {
            throw new DAOException("Item não encontrado para atualização: " + item.getId());
        }
    }

    /**
     * Atualiza quantidade e custo total diretamente
     */
    public void atualizarQuantidadeECusto(Long id, int quantidade, double custoTotal) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        
        if (custoTotal < 0) {
            throw new IllegalArgumentException("Custo total não pode ser negativo");
        }
        
        String sql = "UPDATE estoque SET quantidade = ?, custo_total = ? WHERE id = ?";
        
        int affected = executeUpdate(sql, quantidade, custoTotal, id);
        
        if (affected == 0) {
            throw new DAOException("Item não encontrado: " + id);
        }
    }

    /**
     * Entrada de lote: soma quantidade e custo total
     */
    public void entradaLote(Long id, int quantidade, double custoTotalLote) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        if (custoTotalLote < 0) {
            throw new IllegalArgumentException("Custo total não pode ser negativo");
        }
        
        String sql = "UPDATE estoque SET quantidade = quantidade + ?, custo_total = custo_total + ? WHERE id = ?";
        
        int affected = executeUpdate(sql, quantidade, custoTotalLote, id);
        
        if (affected == 0) {
            throw new DAOException("Item não encontrado: " + id);
        }
        
        logger.info(String.format("Entrada de lote: ID=%d, Qtd=%d, Custo=%.2f", id, quantidade, custoTotalLote));
    }

    /**
     * Saída/consumo: reduz quantidade e custo proporcional
     */
    public void saidaConsumo(Long id, int quantidade) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        // Busca item atual
        Optional<ItemEstoque> opt = buscarPorId(id);
        if (opt.isEmpty()) {
            throw new DAOException("Item não encontrado: " + id);
        }
        
        ItemEstoque item = opt.get();
        
        if (quantidade > item.getQuantidade()) {
            throw new IllegalArgumentException(
                String.format("Quantidade insuficiente em estoque. Disponível: %d, Solicitado: %d",
                    item.getQuantidade(), quantidade)
            );
        }
        
        // Calcula novo custo proporcional
        double custoUnitario = item.getCustoUnitario();
        int novaQuantidade = item.getQuantidade() - quantidade;
        double novoCusto = Math.max(0.0, item.getCustoTotal() - (custoUnitario * quantidade));
        
        String sql = "UPDATE estoque SET quantidade = ?, custo_total = ? WHERE id = ?";
        executeUpdate(sql, novaQuantidade, novoCusto, id);
        
        logger.info(String.format("Saída de estoque: ID=%d, Qtd=%d, Custo Unit=%.2f", id, quantidade, custoUnitario));
    }

    /**
     * Remove um item do estoque
     */
    public void remover(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        String sql = "DELETE FROM estoque WHERE id = ?";
        
        int affected = executeUpdate(sql, id);
        
        if (affected == 0) {
            throw new DAOException("Item não encontrado para remoção: " + id);
        }
        
        logger.info("Item removido do estoque: ID=" + id);
    }

    /**
     * Lista itens com quantidade abaixo de um limite
     */
    public List<ItemEstoque> listarEstoqueBaixo(int quantidadeMinima) {
        String sql = "SELECT * FROM estoque WHERE quantidade <= ? ORDER BY quantidade ASC, nome";
        return executeQuery(sql, this::mapToItemEstoque, quantidadeMinima);
    }

    /**
     * Conta total de itens em estoque
     */
    public int contarItens() {
        String sql = "SELECT COUNT(*) FROM estoque";
        return executeScalar(sql, Integer.class);
    }

    /**
     * Calcula valor total do estoque
     */
    public double calcularValorTotal() {
        String sql = "SELECT COALESCE(SUM(custo_total), 0) FROM estoque";
        return executeScalar(sql, Double.class);
    }

    /**
     * Mapeia ResultSet para ItemEstoque
     */
    private ItemEstoque mapToItemEstoque(ResultSet rs) throws SQLException {
        ItemEstoque item = new ItemEstoque();
        item.setId(rs.getLong("id"));
        item.setNome(rs.getString("nome"));
        item.setCor(rs.getString("cor"));
        item.setTamanho(rs.getString("tamanho"));
        item.setQuantidade(rs.getInt("quantidade"));
        item.setCustoTotal(rs.getDouble("custo_total"));
        return item;
    }

    /**
     * Valida campos obrigatórios do item
     */
    private void validateItem(ItemEstoque item) {
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
}