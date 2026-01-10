package br.com.fotocastro.ui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.util.function.Consumer;

public class MainNavigation {

    private static MainNavigation instance;

    private StackPane contentRoot;
    private Consumer<String> navigationCallback;

    private MainNavigation() {}

    public static MainNavigation getInstance() {
        if (instance == null) {
            instance = new MainNavigation();
        }
        return instance;
    }

    public void setContentRoot(StackPane contentRoot) {
        this.contentRoot = contentRoot;
    }

    public void setView(Node view) {
        if (contentRoot == null) {
            throw new IllegalStateException("contentRoot não foi configurado");
        }
        contentRoot.getChildren().setAll(view);
    }

    // Adicionando método goTo
    public void goTo(String screen) {
        if (navigationCallback != null) {
            navigationCallback.accept(screen);
        } else {
            System.out.println("Navegando para: " + screen);
        }
    }

    // Adicionando registro de callback
    public void register(Consumer<String> callback) {
        this.navigationCallback = callback;
    }

}