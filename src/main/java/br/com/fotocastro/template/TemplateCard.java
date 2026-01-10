package br.com.fotocastro.template;

import br.com.fotocastro.model.TemplateVenda;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Card visual para exibir um template de venda.
 * Design: Card compacto com ícone SVG, nome, preço e tag.
 */
public class TemplateCard extends VBox {
    
    private final TemplateVenda template;
    private Runnable onClickAction;
    private SVGPath iconPath;
    
    public TemplateCard(TemplateVenda template) {
        this.template = template;
        setupCard();
    }
    
    private void setupCard() {
        // Estilo do card
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(8));          // reduzido de 12
        setSpacing(4);                      // reduzido de 8
        setPrefWidth(130);                  // reduzido de 140
        
        // ✅ USA APENAS CLASSES CSS - deixa o CSS decidir as cores
        getStyleClass().add("template-card");
        
        // Cor de borda baseada no ícone
        String borderColor = template.getIcone().getColor();
        
        // ✅ Apenas borda e cursor que não conflitam com dark mode
        setStyle(String.format(
            "-fx-border-color: %s; " +
            "-fx-border-width: 2; " +
            "-fx-cursor: hand;",
            borderColor
        ));
        
        // Ícone SVG no topo
        iconPath = createSVGIcon();
        StackPane iconContainer = new StackPane(iconPath);
        iconContainer.setPrefSize(36, 36);   // reduzido de 40
        iconContainer.setMaxSize(36, 36);
        
        // Nome do template
        Label lblNome = new Label(template.getNome());
        lblNome.setFont(Font.font("System", FontWeight.BOLD, 12)); // reduzido de 13
        lblNome.setWrapText(true);
        lblNome.setMaxWidth(110);           // reduzido
        lblNome.setAlignment(Pos.CENTER);
        lblNome.setStyle("-fx-text-alignment: center;");
        lblNome.getStyleClass().add("template-card-name");
        
        // Preço
        Label lblPreco = new Label(String.format("R$ %.2f", template.getPreco()));
        lblPreco.setFont(Font.font("System", FontWeight.BOLD, 13)); // reduzido de 14
        lblPreco.setStyle("-fx-text-fill: " + borderColor + ";");
        lblPreco.getStyleClass().add("template-card-price");
        
        // Adiciona componentes principais
        getChildren().addAll(iconContainer, lblNome, lblPreco);
        
        // Tamanho (se existir)
        if (template.getTamanho() != null && !template.getTamanho().isEmpty()) {
            Label lblTamanho = new Label(template.getTamanho());
            lblTamanho.setFont(Font.font(9)); // reduzido
            lblTamanho.getStyleClass().add("template-card-size");
            getChildren().add(lblTamanho);
        }
        
        // Tag (categoria) - badge pequeno
        if (template.getTag() != null && !template.getTag().isEmpty()) {
            Label lblTag = new Label(template.getTag());
            lblTag.setFont(Font.font(8)); // reduzido
            lblTag.setPadding(new Insets(2, 6, 2, 6));
            lblTag.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: 8; " +
                "-fx-text-fill: white;",
                borderColor
            ));
            lblTag.getStyleClass().add("template-card-tag");
            getChildren().add(lblTag);
        }
        
        // Alerta de estoque baixo (se aplicável)
        if (template.temEstoqueVinculado() && template.getEstoqueDisponivel() <= 10) {
            Label lblAlerta = new Label("⚠️ Estoque baixo");
            lblAlerta.setFont(Font.font(9));
            lblAlerta.setStyle("-fx-text-fill: #ef4444;");
            getChildren().add(lblAlerta);
        }
        
        // Hover effect
        setOnMouseEntered(e -> {
            setStyle(getStyle() + "-fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });
        
        setOnMouseExited(e -> {
            setStyle(getStyle().replace("-fx-scale-x: 1.05; -fx-scale-y: 1.05;", ""));
        });
        
        // Click handler
        setOnMouseClicked(e -> {
            if (onClickAction != null) {
                onClickAction.run();
            }
        });
    }
    
    /**
     * Cria ícone SVG a partir do template usando path data embutido
     */
    private SVGPath createSVGIcon() {
        SVGPath path = new SVGPath();
        
        // ✅ USA O PATH DATA DO ENUM DIRETAMENTE
        String pathData = template.getIcone().getPathData();
        
        if (pathData != null && !pathData.isEmpty()) {
            path.setContent(pathData);
        } else {
            // Fallback: círculo simples
            path.setContent("M 12 2 A 10 10 0 1 1 12 22 A 10 10 0 1 1 12 2");
        }
        
        // Define a cor do ícone
        path.setFill(Color.web(template.getIcone().getColor()));
        path.getStyleClass().add("template-icon");
        
        // ✅ Ajusta escala para ícones menores
        path.setScaleX(1.4);
        path.setScaleY(1.4);
        
        return path;
    }
    
    public TemplateVenda getTemplate() {
        return template;
    }
    
    public void setOnClick(Runnable action) {
        this.onClickAction = action;
    }
}