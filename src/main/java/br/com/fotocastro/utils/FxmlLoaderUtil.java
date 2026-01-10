package br.com.fotocastro.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class FxmlLoaderUtil {
    public static Parent load(String fxmlPath) throws IOException {
        return FXMLLoader.load(FxmlLoaderUtil.class.getResource(fxmlPath));
    }
}
