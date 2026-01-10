package br.com.fotocastro.infra;

import org.h2.tools.Server;

import java.sql.SQLException;

public class H2ConsoleStarter {
    private static Server server;

    public static void start() {
        try {
            // Porta 8082, abre no navegador automático
            server = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
            server.start();
            System.out.println("H2 Console iniciado: http://localhost:8082");
            System.out.println("JDBC URL: jdbc:h2:file:./data/fotocastro");
            System.out.println("Usuário: sa | Senha: (vazio)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop();
        }
    }
}