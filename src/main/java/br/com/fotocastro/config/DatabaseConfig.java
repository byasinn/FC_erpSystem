package br.com.fotocastro.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Configuração centralizada do banco de dados com HikariCP.
 * Singleton para garantir um único pool de conexões.
 */
public class DatabaseConfig {
    
    private static DatabaseConfig instance;
    private final HikariDataSource dataSource;
    
    private DatabaseConfig() {
        HikariConfig config = new HikariConfig();
        
        // Configurações do H2
        config.setJdbcUrl("jdbc:h2:file:./data/fotocastro;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        
        // Configurações do pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 segundos
        config.setIdleTimeout(600000); // 10 minutos
        config.setMaxLifetime(1800000); // 30 minutos
        
        // Pool name para debug
        config.setPoolName("FotoCastroPool");
        
        // Auto-commit
        config.setAutoCommit(true);
        
        // Validação de conexões
        config.setConnectionTestQuery("SELECT 1");
        
        this.dataSource = new HikariDataSource(config);
    }
    
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().execute("SHUTDOWN");
        } catch (Exception ignored) {}
        dataSource.close();
    }
} 
    // Métodos úteis para informações do pool
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }
    
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }
    
    public int getTotalConnections() {
        return dataSource.getHikariPoolMXBean().getTotalConnections();
    }

}
