package br.com.fotocastro.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class VendaEventBus {

    private static final BooleanProperty VENDA_ATUALIZADA =
            new SimpleBooleanProperty(false);

    public static BooleanProperty vendaAtualizadaProperty() {
        return VENDA_ATUALIZADA;
    }

    public static void notificarVenda() {
        VENDA_ATUALIZADA.set(!VENDA_ATUALIZADA.get());
    }
}
