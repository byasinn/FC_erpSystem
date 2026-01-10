package br.com.fotocastro;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.flywaydb.core.Flyway;

public class Boot extends Application {
    // UMA URL para tudo (Flyway, DAO, Console H2)
    private static final String JDBC_URL = "jdbc:h2:file:./data/fotocastro;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER  = "sa";
    private static final String DB_PASS  = "";

    private void migrate() {
        Flyway.configure()
                .dataSource(JDBC_URL, DB_USER, DB_PASS)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Override
    public void start(Stage stage) {
        try {
            // 1) Migrações
            migrate();

            // 2) UI principal
            var url = getClass().getResource("/ui/MainView.fxml");
            if (url == null) throw new IllegalStateException("FXML não encontrado em /ui/MainView.fxml");

            FXMLLoader fx = new FXMLLoader(url);
            Scene scene = new Scene(fx.load());

            // 3) CSS global
            var css = getClass().getResource("/css/app.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            stage.setScene(scene);
            stage.setTitle("FotoCastro — ERP");

            // 4) Ícones da janela (se existirem)
            try {
                stage.getIcons().addAll(
                        new Image(getClass().getResourceAsStream("/icons/fotocastro-16.png")),
                        new Image(getClass().getResourceAsStream("/icons/fotocastro-32.png")),
                        new Image(getClass().getResourceAsStream("/icons/fotocastro-48.png")),
                        new Image(getClass().getResourceAsStream("/icons/fotocastro-128.png")),
                        new Image(getClass().getResourceAsStream("/icons/fotocastro-256.png"))
                );
            } catch (Exception ignore) {
                // Se não achar os ícones, segue sem quebrar.
            }

            // 5) Tamanho ~70% da tela + centralizado
            Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            double w = vb.getWidth()  * 0.70;
            double h = vb.getHeight() * 0.70;

            stage.setWidth(w);
            stage.setHeight(h);
            stage.setX(vb.getMinX() + (vb.getWidth()  - w) / 2);
            stage.setY(vb.getMinY() + (vb.getHeight() - h) / 2);

            // 6) Usabilidade
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(650);

            // Fecha tudo ao fechar a janela
            stage.setOnCloseRequest(e -> Platform.exit());

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
