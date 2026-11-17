package Techlab.interfaces;

import java.sql.Connection;
import java.sql.SQLException;

/** Copiada da interface do professor. */
public interface IConexaoBD {
    Connection obterConexao() throws SQLException;
    void inicializarBanco();
}
