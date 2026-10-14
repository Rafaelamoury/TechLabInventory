import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChamadoDAO {

    // Metodo responsavel por salvar um chamado no banco
    public void salvar(Chamado chamado) throws SQLException {

        String sql = "INSERT INTO chamados (descricao, ativo_id) "
                   + "VALUES (?, ?)";

        try (Connection conexao = ConexaoBanco.obterConexao()) {

            // Habilitar verificacao de chaves estrangeiras
            try (PreparedStatement pragma =
                    conexao.prepareStatement("PRAGMA foreign_keys = ON")) {

                pragma.execute();
            }

            try (PreparedStatement stmt = conexao.prepareStatement(sql)) {

                stmt.setString(1, chamado.getDescricao());
                stmt.setInt(2, chamado.getAtivoId());

                stmt.executeUpdate();

                System.out.println(
                    "[LOG] Chamado cadastrado com sucesso!"
                );
            }

        } catch (SQLException e) {

            System.err.println(
                "[ERRO DAO] Falha ao cadastrar chamado: "
                + e.getMessage()
            );

            throw e;
        }
    }

    // Metodo responsavel por listar todos os chamados
    public List<Chamado> listarTodos() throws SQLException {

        List<Chamado> chamados = new ArrayList<>();

        String sql = "SELECT id, descricao, ativo_id "
                   + "FROM chamados ORDER BY id";

        try (Connection conexao = ConexaoBanco.obterConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Chamado chamado = new Chamado(
                    rs.getInt("id"),
                    rs.getString("descricao"),
                    rs.getInt("ativo_id")
                );

                chamados.add(chamado);
            }

        } catch (SQLException e) {

            System.err.println(
                "[ERRO DAO] Falha ao listar chamados: "
                + e.getMessage()
            );

            throw e;
        }

        return chamados;
    }
}

