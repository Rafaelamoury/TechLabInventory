package Techlab.service;

import Techlab.interfaces.INotificavel;

import java.io.PrintStream;
import java.util.Objects;

/** Implementacao concreta de INotificavel para o sistema de terminal. */
public final class NotificadorConsole implements INotificavel {
    private final PrintStream destino;

    public NotificadorConsole() {
        this(System.out);
    }

    public NotificadorConsole(PrintStream destino) {
        this.destino = Objects.requireNonNull(destino, "Destino da notificacao obrigatorio.");
    }

    @Override
    public void notificar(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("Mensagem de notificacao obrigatoria.");
        }
        destino.println("[NOTIFICACAO] " + mensagem);
    }
}
