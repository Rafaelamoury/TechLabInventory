package Techlab.exceptions;

import java.sql.SQLException;

/** Ausencia de chamado ao atualizar ou excluir. */
public final class ChamadoNaoEncontradoException extends SQLException {
    public ChamadoNaoEncontradoException(int id) {
        super("Chamado nao encontrado: " + id);
    }
}
