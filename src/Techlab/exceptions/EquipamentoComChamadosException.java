package Techlab.exceptions;

import java.sql.SQLException;

/** Exclusao recusada porque existem chamados associados ao equipamento. */
public final class EquipamentoComChamadosException extends SQLException {
    public EquipamentoComChamadosException(int id) {
        super("Nao e permitido excluir ativo " + id + ": existem chamados vinculados.");
    }
}
