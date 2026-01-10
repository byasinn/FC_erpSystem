package br.com.fotocastro.infra;

import br.com.fotocastro.model.PaymentMethod;
import br.com.fotocastro.model.Venda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.math.BigDecimal;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;


/**
 * DAO para gerenciamento de vendas.
 * Versão refatorada usando DAOBase e connection pool.
 */
public class VendaDaoH2 extends DAOBase {

    /**
     * Insere uma nova venda
     */
    public Long inserir(Venda venda) {
        validateVenda(venda);
        
        String sql = "INSERT INTO venda (data_hora, descricao, valor_bruto, metodo, taxa, valor_liq, observacao) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        LocalDateTime dataHora = venda.getDataHora() != null ? venda.getDataHora() : LocalDateTime.now();
        
        return executeInsert(sql,
            Timestamp.valueOf(dataHora),
            venda.getDescricao(),
            venda.getValorBruto(),
            venda.getMetodo().name(),
            venda.getTaxa(),
            venda.getValorLiquido(),
            null // observacao
        );
    }

    /**
     * Atualiza uma venda existente
     */
    public void atualizar(Venda venda) {
        validateVenda(venda);
        
        if (venda.getId() == null) {
            throw new IllegalArgumentException("ID da venda não pode ser nulo");
        }
        
        String sql = "UPDATE venda SET descricao = ?, valor_bruto = ?, metodo = ?, taxa = ?, valor_liq = ? WHERE id = ?";
        
        int affected = executeUpdate(sql,
            venda.getDescricao(),
            venda.getValorBruto(),
            venda.getMetodo().name(),
            venda.getTaxa(),
            venda.getValorLiquido(),
            venda.getId()
        );
        
        if (affected == 0) {
            throw new DAOException("Venda não encontrada para atualização: " + venda.getId());
        }
    }

    /**
     * Remove uma venda
     */
    public void remover(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        String sql = "DELETE FROM venda WHERE id = ?";
        
        int affected = executeUpdate(sql, id);
        
        if (affected == 0) {
            throw new DAOException("Venda não encontrada para remoção: " + id);
        }
        
        logger.info("Venda removida: ID=" + id);
    }

    /**
     * Busca venda por ID
     */
    public Optional<Venda> buscarPorId(Long id) {
        String sql = "SELECT * FROM venda WHERE id = ?";
        return executeQuerySingle(sql, this::mapToVenda, id);
    }

    /**
     * Lista vendas recentes (limitado)
     */
    public List<Venda> listarRecentes(int limit) {
        String sql = "SELECT * FROM venda ORDER BY data_hora DESC LIMIT ?";
        return executeQuery(sql, this::mapToVenda, limit);
    }

    /**
     * Lista todas as vendas
     */
    public List<Venda> listarTodas() {
        String sql = "SELECT * FROM venda ORDER BY data_hora DESC";
        return executeQuery(sql, this::mapToVenda);
    }

    /**
     * Lista vendas por período
     */
    public List<Venda> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT * FROM venda WHERE CAST(data_hora AS DATE) BETWEEN ? AND ? ORDER BY data_hora DESC";
        return executeQuery(sql, this::mapToVenda, inicio, fim);
    }

    /**
     * Lista vendas de hoje
     */
    public List<Venda> listarHoje() {
        String sql = "SELECT * FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE ORDER BY data_hora DESC";
        return executeQuery(sql, this::mapToVenda);
    }

    // ========== MÉTODOS DE AGREGAÇÃO ==========

    /**
     * Soma total bruto de todas as vendas
     */
    public double somaBruto() {
        String sql = "SELECT COALESCE(SUM(valor_bruto), 0.0) FROM venda";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd == null ? 0.0 : bd.doubleValue();
    }

    /**
     * Soma total de taxas
     */
    public double somaTaxas() {
        String sql = "SELECT COALESCE(SUM(taxa), 0.0) FROM venda";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    /**
     * Soma total líquido
     */
    public double somaLiquido() {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    /**
     * Soma líquido por método de pagamento
     */
    public double somaPorMetodo(PaymentMethod metodo) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE metodo = ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, metodo.name());
        return bd != null ? bd.doubleValue() : 0.0;
    }

    /**
     * Soma líquido das vendas de hoje
     */
    public double somaLiquidoHoje() {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    /**
     * Conta número de vendas de hoje
     */
    public int contarVendasHoje() {
        String sql = "SELECT COUNT(*) FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        return executeScalar(sql, Integer.class);
    }

    /**
     * Soma por período
     */
    public double somaPorPeriodo(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE CAST(data_hora AS DATE) BETWEEN ? AND ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, inicio, fim);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    /**
     * Estatísticas por método de pagamento (hoje)
     */
    public double somaPorMetodoHoje(PaymentMethod metodo) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE metodo = ? AND CAST(data_hora AS DATE) = CURRENT_DATE";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, metodo.name());
        return bd != null ? bd.doubleValue() : 0.0;
    }

    // ========== MAPPERS ==========

    /**
     * Mapeia ResultSet para Venda
     */
    private Venda mapToVenda(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.setId(rs.getLong("id"));
        v.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
        v.setDescricao(rs.getString("descricao"));
        v.setValorBruto(rs.getDouble("valor_bruto"));
        v.setMetodo(PaymentMethod.valueOf(rs.getString("metodo")));
        v.setTaxa(rs.getDouble("taxa"));
        v.setValorLiquido(rs.getDouble("valor_liq"));
        return v;
    }

    public Map<LocalDate, Double> vendasLiquidasPorDiaUltimos30() {
    String sql = """
        SELECT
            CAST(data_hora AS DATE) AS dia,
            COALESCE(SUM(valor_liq), 0.0) AS total
        FROM venda
        WHERE data_hora >= DATEADD('DAY', -30, CURRENT_TIMESTAMP)
        GROUP BY dia
        ORDER BY dia DESC

        """;

    List<Map.Entry<LocalDate, Double>> rows =
        executeQuery(sql, rs ->
            Map.entry(
                rs.getDate("dia").toLocalDate(),
                rs.getDouble("total")
            )
        );

    Map<LocalDate, Double> result = new LinkedHashMap<>();
    for (var e : rows) {
        result.put(e.getKey(), e.getValue());
    }
    return result;
}

    public Map<Integer, Double> vendasPorHora() {
    String sql = """
        SELECT EXTRACT(HOUR FROM data_hora) AS hora, COALESCE(SUM(valor_liq), 0.0) AS total
        FROM venda
        GROUP BY hora
        ORDER BY hora
        """;

    List<Map.Entry<Integer, Double>> rows =
        executeQuery(sql, rs ->
            Map.entry(
                rs.getInt("hora"),
                rs.getDouble("total")
            )
        );

    Map<Integer, Double> result = new LinkedHashMap<>();
    for (var e : rows) {
        result.put(e.getKey(), e.getValue());
    }
    return result;
}


    public List<Map.Entry<String, Long>> topDescricoes(int limit) {
    String sql = """
        SELECT descricao, COUNT(*) AS qtd
        FROM venda
        WHERE descricao IS NOT NULL AND TRIM(descricao) != ''
        GROUP BY descricao
        ORDER BY qtd DESC
        LIMIT ?
        """;

    return executeQuery(sql, rs ->
        Map.entry(
            rs.getString("descricao"),
            rs.getLong("qtd")
        ),
        limit
    );
}


    public boolean caixaJaFechado(LocalDate data) {
    String sql = "SELECT COUNT(*) FROM fechamento_caixa WHERE data = ?";
    Long count = executeScalar(sql, Long.class, data);
    return count != null && count > 0;

}

    public void inserirFechamento(LocalDate data, double bruto, double taxas, double liquido,
                                double dinheiro, double cartao, double pix) {
        String sql = "INSERT INTO fechamento_caixa (data, bruto, taxas, liquido, dinheiro, cartao, pix) VALUES (?, ?, ?, ?, ?, ?, ?)";
        executeInsert(sql, data, bruto, taxas, liquido, dinheiro, cartao, pix);
    }

    // Métodos "Hoje" (se não existirem)
    public double somaBrutoHoje() {
        String sql = "SELECT COALESCE(SUM(valor_bruto), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaTaxasHoje() {
        String sql = "SELECT COALESCE(SUM(taxa), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public void limparVendasHoje() {
    String sql = "DELETE FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
    int affected = executeUpdate(sql);
    logger.info("Vendas de hoje limpas: " + affected + " registros removidos");
}

    public double somaBrutoPorDia(LocalDate data) {
        String sql = "SELECT COALESCE(SUM(valor_bruto), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, data);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaTaxasPorDia(LocalDate data) {
        String sql = "SELECT COALESCE(SUM(taxa), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, data);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaLiquidoPorDia(LocalDate data) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, data);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaPorMetodoPorDia(PaymentMethod metodo, LocalDate data) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE metodo = ? AND CAST(data_hora AS DATE) = ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, metodo.name(), data);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public void inserirFechamentoAutomatico(LocalDate data, double bruto, double taxas, double liquido,
                                            double dinheiro, double cartao, double pix) {
        String sql = "INSERT INTO fechamento_caixa (data, bruto, taxas, liquido, dinheiro, cartao, pix, automatico) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
        executeInsert(sql, data, bruto, taxas, liquido, dinheiro, cartao, pix);
        logger.info("Fechamento automático inserido para " + data);
    }

    /**
     * Valida campos obrigatórios da venda
     */
    private void validateVenda(Venda venda) {
        if (venda == null) {
            throw new IllegalArgumentException("Venda não pode ser nula");
        }
        
        if (venda.getDescricao() == null || venda.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da venda é obrigatória");
        }
        
        if (venda.getValorBruto() < 0) {
            throw new IllegalArgumentException("Valor bruto não pode ser negativo");
        }
        
        if (venda.getMetodo() == null) {
            throw new IllegalArgumentException("Método de pagamento é obrigatório");
        }
        
        if (venda.getTaxa() < 0) {
            throw new IllegalArgumentException("Taxa não pode ser negativa");
        }
        
        if (venda.getValorLiquido() < 0) {
            throw new IllegalArgumentException("Valor líquido não pode ser negativo");
        }
    }
}