package Techlab.exceptions;

import java.sql.SQLException;

/** Falha de comunicacao/abertura do banco, preservando a causa JDBC. */
public final class FalhaConexaoBDException extends SQLException {
    public FalhaConexaoBDException(String descricao, SQLException causa) {
        super(descricao, causa.getSQLState(), causa.getErrorCode(), causa);
    }
}
