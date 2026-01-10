package br.com.fotocastro.infra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe base para todos os DAOs.
 * Fornece acesso ao DataSource e métodos utilitários comuns.
 */
public abstract class DAOBase {
    
    protected static final Logger logger = Logger.getLogger(DAOBase.class.getName());
    protected final DatabaseConfig dbConfig;
    
    protected DAOBase() {
        this.dbConfig = DatabaseConfig.getInstance();
    }
    
    /**
     * Obtém uma conexão do pool
     */
    protected Connection getConnection() throws SQLException {
        return dbConfig.getConnection();
    }
    
    /**
     * Executa uma query que retorna uma lista de objetos
     */
    protected <T> List<T> executeQuery(String sql, RowMapper<T> mapper, Object... params) {
        List<T> result = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao executar query: " + sql, e);
            throw new DAOException("Erro ao executar consulta", e);
        }
        
        return result;
    }
    
    /**
     * Executa uma query que retorna um único objeto opcional
     */
    protected <T> Optional<T> executeQuerySingle(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper.map(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao executar query single: " + sql, e);
            throw new DAOException("Erro ao executar consulta", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Executa um UPDATE, INSERT ou DELETE
     * @return número de linhas afetadas
     */
    protected int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            return ps.executeUpdate();
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao executar update: " + sql, e);
            throw new DAOException("Erro ao executar atualização", e);
        }
    }
    
    /**
     * Executa um INSERT e retorna a chave gerada
     */
    protected Long executeInsert(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            setParameters(ps, params);
            int affected = ps.executeUpdate();
            
            if (affected == 0) {
                throw new DAOException("INSERT falhou, nenhuma linha afetada");
            }
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new DAOException("INSERT falhou, nenhum ID gerado");
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao executar insert: " + sql, e);
            throw new DAOException("Erro ao executar inserção", e);
        }
    }
    
    /**
     * Executa uma query escalar (retorna um único valor)
     */
    protected <T> T executeScalar(String sql, Class<T> type, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);
                    if (value == null) {
                        return getDefaultValue(type);
                    }
                    return type.cast(value);
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao executar scalar: " + sql, e);
            throw new DAOException("Erro ao executar consulta escalar", e);
        }
        
        return getDefaultValue(type);
    }
    
    /**
     * Define os parâmetros no PreparedStatement
     */
    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Retorna valor padrão para tipos primitivos
     */
    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(Class<T> type) {
        if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(0);
        }
        if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(0L);
        }
        if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(0.0);
        }
        if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.FALSE;
        }
        return null;
    }
    
    /**
     * Interface funcional para mapear ResultSet -> Objeto
     */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
    
    /**
     * Exception customizada para erros de DAO
     */
    public static class DAOException extends RuntimeException {
        public DAOException(String message) {
            super(message);
        }
        
        public DAOException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}