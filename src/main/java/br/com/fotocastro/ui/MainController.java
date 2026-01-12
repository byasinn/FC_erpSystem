package br.com.fotocastro.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.fotocastro.config.DatabaseConfig;

public class MainController {

    @FXML private Label lblUser;
    @FXML private Label lblStatus;
    @FXML private Label lblDateTime;
    @FXML private Button btnCaixa;
    @FXML private Button btnEstoque;
    @FXML private StackPane contentRoot;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Timeline clock;
    private final Map<String, Node> cache = new LinkedHashMap<>();
    private boolean dark = false;

    @FXML
    private void initialize() {
        safeSetText(lblStatus, "Pronto.");
        startClock();

        // configura a navegação e contentRoot após a cena estar pronta
        Platform.runLater(() -> {
            if (contentRoot != null) {
                MainNavigation.getInstance().setContentRoot(contentRoot);
                registrarNavegacao();
                Scene scene = contentRoot.getScene();
                if (scene != null) {
                    registrarAtalhos(scene);
                }
                showHome();
            }
        });
    }

    private void startClock() {
        safeSetText(lblDateTime, LocalDateTime.now().format(fmt));
        clock = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> safeSetText(lblDateTime, LocalDateTime.now().format(fmt))));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void setLoggedUser(String username) {
        String label = (username != null && !username.isBlank()) ? "Usuário: " + username : "Usuário: (desconhecido)";
        safeSetText(lblUser, label);
    }

    /* ========= Navegação ========= */

    @FXML
    public void showHome() {
        setCenter(loadView("/ui/HomeView.fxml"));
        safeSetText(lblStatus, "Início.");
    }

    @FXML
    public void showCaixa() {
        setCenter(loadView("/ui/CaixaView.fxml"));
        safeSetText(lblStatus, "Caixa.");
    }

    @FXML
    public void showEstoque() {
        setCenter(loadView("/ui/EstoqueView.fxml"));
        safeSetText(lblStatus, "Estoque.");
    }

    @FXML
    public void showDashboard() {
        setCenter(loadView("/ui/DashboardView.fxml"));
        safeSetText(lblStatus, "Dashboard.");
    }

    @FXML
    public void toggleTheme() {
        if (contentRoot == null || contentRoot.getScene() == null) return;
        var root = contentRoot.getScene().getRoot();
        
        if (dark) {
            root.getStyleClass().remove("dark");
            dark = false;
            safeSetText(lblStatus, "Tema: Claro");
        } else {
            if (!root.getStyleClass().contains("dark")) {
                root.getStyleClass().add("dark");
            }
            dark = true;
            safeSetText(lblStatus, "Tema: Escuro");
        }
        
        // 🔥 CRÍTICO: Limpa cache e recarrega a view atual
        String currentView = getCurrentViewPath();
        cache.clear(); // Limpa o cache
        if (currentView != null) {
            setCenter(loadView(currentView)); // Recarrega a view
        }
    }

    // Método auxiliar para saber qual view está ativa
    private String getCurrentViewPath() {
        if (contentRoot == null || contentRoot.getChildren().isEmpty()) {
            return null;
        }
        
        // Detecta qual view está ativa pelo cache
        for (Map.Entry<String, Node> entry : cache.entrySet()) {
            if (contentRoot.getChildren().contains(entry.getValue())) {
                return entry.getKey();
            }
        }
        
        return "/ui/HomeView.fxml"; // Default
    }

    private void registrarNavegacao() {
        MainNavigation.getInstance().register(this::navigate);
    }

    private void navigate(String route) {
        switch (route) {
            case "HOME" -> showHome();
            case "CAIXA" -> showCaixa();
            case "ESTOQUE" -> showEstoque();
            case "DASHBOARD" -> showDashboard();
            case "CAIXA_FECHAMENTO" -> showCaixa(); // depois pode ter tela própria
            default -> showHome();
        }
    }

    /* ========= Carregamento de views ========= */

    private void setCenter(Node node) {
        if (node != null && contentRoot != null) {
            contentRoot.getChildren().setAll(node);
        }
    }

    private Node loadView(String fxmlPath) {
        try {
            // if (cache.containsKey(fxmlPath)) return cache.get(fxmlPath);

            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalStateException("FXML não encontrado: " + fxmlPath);

            Node node = FXMLLoader.load(url);

            // Configura ações dos botões da HomeView
            if (fxmlPath.equals("/ui/HomeView.fxml")) {
                Button btnAbrirCaixa = (Button) node.lookup("#btnAbrirCaixa");
                if (btnAbrirCaixa != null) btnAbrirCaixa.setOnAction(e -> showCaixa());

                Button btnDashboard = (Button) node.lookup("#btnDashboard");
                if (btnDashboard != null) btnDashboard.setOnAction(e -> showDashboard());

                Button btnEstoque = (Button) node.lookup("#btnEstoque");
                if (btnEstoque != null) btnEstoque.setOnAction(e -> showEstoque());
            }

            // cache.put(fxmlPath, node);
            return node;
        } catch (Exception e) {
            e.printStackTrace();
            safeSetText(lblStatus, "Erro ao carregar: " + fxmlPath);
            return new Label("Falha ao carregar a tela.");
        }
    }

    private void registrarAtalhos(Scene scene) {
        if (scene == null) return;

        scene.getAccelerators().put(KeyCombination.keyCombination("F1"), this::showCaixa);
        scene.getAccelerators().put(KeyCombination.keyCombination("F2"), this::showEstoque);
        scene.getAccelerators().put(KeyCombination.keyCombination("ESCAPE"), this::handleExit);
    }

    /* ========= Sistema ========= */

    @FXML
    public void handleExit() {
        if (clock != null) clock.stop();
        DatabaseConfig.getInstance().shutdown();
        Platform.exit();
    }

    private void safeSetText(Label label, String text) {
        if (label != null) label.setText(text);
    }

}