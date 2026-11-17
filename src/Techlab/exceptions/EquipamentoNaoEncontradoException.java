package Techlab.exceptions;

import java.sql.SQLException;

/** Ausencia de um equipamento em operacao que exige um registro existente. */
public final class EquipamentoNaoEncontradoException extends SQLException {
    public EquipamentoNaoEncontradoException(int id) {
        super("Equipamento nao encontrado: " + id);
    }
}
