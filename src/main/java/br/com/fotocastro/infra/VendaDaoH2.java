package br.com.fotocastro.infra;

import br.com.fotocastro.model.PaymentMethod;
import br.com.fotocastro.model.Venda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
/**
 * DAO para gerenciamento de vendas e fechamentos de caixa.
 */
public class VendaDaoH2 extends DAOBase {

    // ========== VENDAS ==========

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

    public Optional<Venda> buscarPorId(Long id) {
        String sql = "SELECT * FROM venda WHERE id = ?";
        return executeQuerySingle(sql, this::mapToVenda, id);
    }

    public List<Venda> listarRecentes(int limit) {
        String sql = "SELECT * FROM venda ORDER BY data_hora DESC LIMIT ?";
        return executeQuery(sql, this::mapToVenda, limit);
    }

    public List<Venda> listarTodas() {
        String sql = "SELECT * FROM venda ORDER BY data_hora DESC";
        return executeQuery(sql, this::mapToVenda);
    }

    public List<Venda> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT * FROM venda WHERE CAST(data_hora AS DATE) BETWEEN ? AND ? ORDER BY data_hora DESC";
        return executeQuery(sql, this::mapToVenda, inicio, fim);
    }

    public List<Venda> listarHoje() {
        String sql = "SELECT * FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE ORDER BY data_hora DESC";
        return executeQuery(sql, this::mapToVenda);
    }

    // ========== AGREGAÇÕES VENDAS ==========

    public double somaBruto() {
        String sql = "SELECT COALESCE(SUM(valor_bruto), 0.0) FROM venda";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaTaxas() {
        String sql = "SELECT COALESCE(SUM(taxa), 0.0) FROM venda";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaLiquido() {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaPorMetodo(PaymentMethod metodo) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE metodo = ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, metodo.name());
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaLiquidoHoje() {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        BigDecimal bd = executeScalar(sql, BigDecimal.class);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public int contarVendasHoje() {
        String sql = "SELECT COUNT(*) FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        return executeScalar(sql, Integer.class);
    }

    public double somaPorPeriodo(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE CAST(data_hora AS DATE) BETWEEN ? AND ?";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, inicio, fim);
        return bd != null ? bd.doubleValue() : 0.0;
    }

    public double somaPorMetodoHoje(PaymentMethod metodo) {
        String sql = "SELECT COALESCE(SUM(valor_liq), 0.0) FROM venda WHERE metodo = ? AND CAST(data_hora AS DATE) = CURRENT_DATE";
        BigDecimal bd = executeScalar(sql, BigDecimal.class, metodo.name());
        return bd != null ? bd.doubleValue() : 0.0;
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

    public void limparVendasHoje() {
        String sql = "DELETE FROM venda WHERE CAST(data_hora AS DATE) = CURRENT_DATE";
        int affected = executeUpdate(sql);
        logger.info("Vendas de hoje limpas: " + affected + " registros removidos");
    }

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

    // ========== FECHAMENTOS DE CAIXA ==========

    public void inserirFechamento(LocalDate data, double bruto, double taxas, double liquido,
                                  double dinheiro, double cartao, double pix) {
        String sql = "INSERT INTO fechamento_caixa (data, bruto, taxas, liquido, dinheiro, cartao, pix) VALUES (?, ?, ?, ?, ?, ?, ?)";
        executeInsert(sql, data, bruto, taxas, liquido, dinheiro, cartao, pix);
    }

    public void inserirFechamentoAutomatico(LocalDate data, double bruto, double taxas, double liquido,
                                            double dinheiro, double cartao, double pix) {
        String sql = "INSERT INTO fechamento_caixa (data, bruto, taxas, liquido, dinheiro, cartao, pix, automatico) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
        executeInsert(sql, data, bruto, taxas, liquido, dinheiro, cartao, pix);
        logger.info("Fechamento automático inserido para " + data);
    }

    public List<FechamentoResumo> listarFechamentos(int limit) {
        String sql = """
            SELECT 
                data,
                fechado_em,
                bruto,
                taxas,
                liquido,
                dinheiro,
                cartao,
                pix,
                valor_contado,
                diferenca,
                observacao,
                automatico
            FROM fechamento_caixa
            ORDER BY data DESC
            LIMIT ?
        """;

        return executeQuery(sql, rs -> {
            LocalDate data = rs.getDate("data").toLocalDate();
            Timestamp fechadoEmTs = rs.getTimestamp("fechado_em");
            LocalDateTime fechadoEm = fechadoEmTs != null ? fechadoEmTs.toLocalDateTime() : null;
            double bruto = rs.getDouble("bruto");
            double liquido = rs.getDouble("liquido");
            double totalPagamentos = rs.getDouble("dinheiro") + rs.getDouble("cartao") + rs.getDouble("pix");
            double valorContado = rs.getDouble("valor_contado");
            double diferenca = rs.getDouble("diferenca");
            String obs = rs.getString("observacao");
            boolean auto = rs.getBoolean("automatico");

            return new FechamentoResumo(data, fechadoEm, bruto, liquido, totalPagamentos,
                                        valorContado, diferenca, obs, auto ? "Automático" : "Manual");
        }, limit);
    }

    public Optional<FechamentoResumo> buscarFechamentoPorData(LocalDate data) {
        String sql = """
            SELECT 
                data,
                fechado_em,
                bruto,
                taxas,
                liquido,
                dinheiro,
                cartao,
                pix,
                valor_contado,
                diferenca,
                observacao,
                automatico
            FROM fechamento_caixa
            WHERE data = ?
        """;

        return executeQuerySingle(sql, rs -> {
            // mesmo mapping do listar
            LocalDate d = rs.getDate("data").toLocalDate();
            Timestamp ts = rs.getTimestamp("fechado_em");
            LocalDateTime fe = ts != null ? ts.toLocalDateTime() : null;
            double b = rs.getDouble("bruto");
            double l = rs.getDouble("liquido");
            double tp = rs.getDouble("dinheiro") + rs.getDouble("cartao") + rs.getDouble("pix");
            double vc = rs.getDouble("valor_contado");
            double dif = rs.getDouble("diferenca");
            String o = rs.getString("observacao");
            boolean a = rs.getBoolean("automatico");

            return new FechamentoResumo(d, fe, b, l, tp, vc, dif, o, a ? "Automático" : "Manual");
        }, data);
    }

    public boolean caixaJaFechado(LocalDate data) {
        String sql = "SELECT COUNT(*) FROM fechamento_caixa WHERE data = ?";
        Long count = executeScalar(sql, Long.class, data);
        return count != null && count > 0;
    }

    // ========== MAPPERS ==========

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

    // ========== GRÁFICOS E ESTATÍSTICAS (já existentes) ==========

    public Map<LocalDate, Double> vendasLiquidasPorDiaUltimos30() {
        String sql = """
            SELECT CAST(data_hora AS DATE) AS dia, COALESCE(SUM(valor_liq), 0.0) AS total
            FROM venda
            WHERE data_hora >= DATEADD('DAY', -30, CURRENT_TIMESTAMP)
            GROUP BY dia
            ORDER BY dia DESC
        """;

        List<Map.Entry<LocalDate, Double>> rows = executeQuery(sql, rs ->
            Map.entry(rs.getDate("dia").toLocalDate(), rs.getDouble("total"))
        );

        Map<LocalDate, Double> result = new LinkedHashMap<>();
        for (var e : rows) result.put(e.getKey(), e.getValue());
        return result;
    }

    public Map<Integer, Double> vendasPorHora() {
        String sql = """
            SELECT EXTRACT(HOUR FROM data_hora) AS hora, COALESCE(SUM(valor_liq), 0.0) AS total
            FROM venda
            GROUP BY hora
            ORDER BY hora
        """;

        List<Map.Entry<Integer, Double>> rows = executeQuery(sql, rs ->
            Map.entry(rs.getInt("hora"), rs.getDouble("total"))
        );

        Map<Integer, Double> result = new LinkedHashMap<>();
        for (var e : rows) result.put(e.getKey(), e.getValue());
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

        return executeQuery(sql, rs -> Map.entry(rs.getString("descricao"), rs.getLong("qtd")), limit);
    }

    private void validateVenda(Venda venda) {
        if (venda == null) throw new IllegalArgumentException("Venda não pode ser nula");
        if (venda.getDescricao() == null || venda.getDescricao().trim().isEmpty()) 
            throw new IllegalArgumentException("Descrição da venda é obrigatória");
        if (venda.getValorBruto() < 0) throw new IllegalArgumentException("Valor bruto não pode ser negativo");
        if (venda.getMetodo() == null) throw new IllegalArgumentException("Método de pagamento é obrigatório");
        if (venda.getTaxa() < 0) throw new IllegalArgumentException("Taxa não pode ser negativa");
        if (venda.getValorLiquido() < 0) throw new IllegalArgumentException("Valor líquido não pode ser negativo");
    }

    // Classe auxiliar para fechamentos (pode mover pra fora se quiser)
    public static class FechamentoResumo {
        public final LocalDate data;
        public final LocalDateTime fechadoEm;
        public final double bruto;
        public final double liquido;
        public final double totalPagamentos;
        public final double valorContado;
        public final double diferenca;
        public final String observacao;
        public final String tipo;

        public FechamentoResumo(LocalDate data, LocalDateTime fechadoEm, double bruto, double liquido,
                                double totalPagamentos, double valorContado, double diferenca,
                                String observacao, String tipo) {
            this.data = data;
            this.fechadoEm = fechadoEm;
            this.bruto = bruto;
            this.liquido = liquido;
            this.totalPagamentos = totalPagamentos;
            this.valorContado = valorContado;
            this.diferenca = diferenca;
            this.observacao = observacao != null ? observacao : "";
            this.tipo = tipo;
        }

        public String getData() { return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        public String getFechadoEm() { return fechadoEm != null ? fechadoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"; }
        public String getBruto() { return String.format("R$ %.2f", bruto); }
        public String getLiquido() { return String.format("R$ %.2f", liquido); }
        public String getTotalPagamentos() { return String.format("R$ %.2f", totalPagamentos); }
        public String getDiferenca() { 
            return diferenca == 0 ? "OK" : String.format("%s%.2f", diferenca > 0 ? "+" : "", diferenca); 
        }
        public String getObservacao() { return observacao; }
        public String getTipo() { return tipo; }
    }
}