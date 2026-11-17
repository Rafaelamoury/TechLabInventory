package Techlab.repository;

import Techlab.dto.AtivoCsvRegistro;
import Techlab.interfaces.IConexaoBD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistencia CSV de ativos baseada no laboratorio do professor.
 * UTF-8, BufferedReader/BufferedWriter e try-with-resources.
 *
 * O CSV registra TODOS os campos do projeto final (inclusive os nulos de
 * cadastros legados); a importacao nunca altera registros existentes.
 */
public final class AtivoArquivoRepository {
    private static final String CABECALHO = "id;patrimonio;nome;categoria;detalhe_especifico;status;"
            + "data_aquisicao;ip_estatico;sistema_operacional;memoria_ram_gb;departamento;"
            + "responsavel;numero_portas";
    private static final String COLUNAS = "id, patrimonio, nome, categoria, detalhe_especifico, "
            + "status, data_aquisicao, ip_estatico, sistema_operacional, memoria_ram_gb, "
            + "departamento, responsavel, numero_portas";
    private static final String INSERT = "INSERT INTO ativos (patrimonio, nome, categoria, detalhe_especifico, "
            + "status, data_aquisicao, ip_estatico, sistema_operacional, memoria_ram_gb, "
            + "departamento, responsavel, numero_portas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private final IConexaoBD banco;

    public AtivoArquivoRepository(IConexaoBD banco) {
        if (banco == null) throw new IllegalArgumentException("Conexao obrigatoria.");
        this.banco = banco;
    }

    public List<AtivoCsvRegistro> lerDoBanco() throws SQLException {
        List<AtivoCsvRegistro> lista = new ArrayList<>();
        try (Connection conn = banco.obterConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + COLUNAS + " FROM ativos ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new AtivoCsvRegistro(
                        rs.getInt("id"), rs.getString("patrimonio"), rs.getString("nome"),
                        rs.getString("categoria"), rs.getString("detalhe_especifico"),
                        rs.getString("status"), rs.getString("data_aquisicao"),
                        rs.getString("ip_estatico"), rs.getString("sistema_operacional"),
                        inteiroOpcional(rs, "memoria_ram_gb"),
                        rs.getString("departamento"), rs.getString("responsavel"),
                        inteiroOpcional(rs, "numero_portas")));
            }
        }
        return lista;
    }

    private static Integer inteiroOpcional(ResultSet rs, String coluna) throws SQLException {
        int numero = rs.getInt(coluna);
        return rs.wasNull() ? null : numero;
    }

    /** Gera um arquivo novo. Nao sobrescreve um CSV anterior. */
    public int exportarBanco(Path caminho) throws SQLException, IOException {
        List<AtivoCsvRegistro> lista = lerDoBanco();
        salvarTodos(lista, caminho);
        return lista.size();
    }

    public void salvarTodos(List<AtivoCsvRegistro> registros, Path caminho) throws IOException {
        if (registros == null || caminho == null)
            throw new IllegalArgumentException("Lista e caminho obrigatorios.");
        Path destino = caminho.toAbsolutePath().normalize();
        if (destino.getParent() != null) Files.createDirectories(destino.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writer.write(CABECALHO);
            writer.newLine();
            for (AtivoCsvRegistro a : registros) {
                if (a == null) throw new IllegalArgumentException("CSV nao aceita registro nulo.");
                String[] valores = {
                        Integer.toString(a.id()), a.patrimonio(), a.modelo(), a.categoria(),
                        a.detalheEspecifico(), a.status(), a.dataAquisicao(), a.ipEstatico(),
                        a.sistemaOperacional(), numero(a.memoriaRamGb()), a.departamento(),
                        a.responsavel(), numero(a.numeroPortas())};
                List<String> texto = new ArrayList<>();
                for (String valor : valores) texto.add(escapar(valor));
                writer.write(String.join(";", texto));
                writer.newLine();
            }
        }
    }

    public List<AtivoCsvRegistro> carregarTodos(Path caminho) throws IOException {
        if (caminho == null) throw new IllegalArgumentException("Caminho obrigatorio.");
        List<AtivoCsvRegistro> lista = new ArrayList<>();
        Set<String> patrimonios = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            String cabecalho = reader.readLine();
            if (!CABECALHO.equals(cabecalho)) throw new IOException("Cabecalho CSV inesperado.");
            String linha;
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) continue;
                try {
                    List<String> partes = dividir(linha);
                    if (partes.size() != 13) throw new IllegalArgumentException("Esperadas 13 colunas, vieram " + partes.size());
                    AtivoCsvRegistro registro = new AtivoCsvRegistro(
                            Integer.parseInt(partes.get(0)), texto(partes.get(1)), texto(partes.get(2)),
                            texto(partes.get(3)), texto(partes.get(4)), texto(partes.get(5)),
                            texto(partes.get(6)), texto(partes.get(7)), texto(partes.get(8)),
                            parseInteiro(partes.get(9)), texto(partes.get(10)), texto(partes.get(11)),
                            parseInteiro(partes.get(12)));
                    if (!patrimonios.add(registro.patrimonio()))
                        throw new IllegalArgumentException("Patrimonio duplicado no CSV: " + registro.patrimonio());
                    lista.add(registro);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Linha " + numeroLinha + " do CSV invalida: " + e.getMessage(), e);
                }
            }
        }
        return lista;
    }

    /**
     * Insere somente patrimonios inexistentes. Nunca substitui ativos atuais.
     * Ignora IDs de origem (o SQLite atribui novos IDs) e usa transacao:
     * qualquer erro reverte todas as insercoes desta importacao.
     */
    public ResultadoImportacao importarNovos(Path caminho) throws IOException, SQLException {
        List<AtivoCsvRegistro> registros = carregarTodos(caminho);
        int inseridos = 0;
        int existentes = 0;
        try (Connection conn = banco.obterConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement buscar = conn.prepareStatement("SELECT id FROM ativos WHERE patrimonio = ?");
                 PreparedStatement inserir = conn.prepareStatement(INSERT)) {
                for (AtivoCsvRegistro r : registros) {
                    buscar.setString(1, r.patrimonio());
                    try (ResultSet rs = buscar.executeQuery()) {
                        if (rs.next()) { existentes++; continue; }
                    }
                    // Novos cadastros devem ser completos. Legados ja presentes no
                    // banco podem continuar incompletos, sem serem modificados.
                    if (!r.completo()) throw new IOException("Patrimonio " + r.patrimonio()
                            + " nao pode ser importado como NOVO: faltam " + String.join(", ", r.pendencias()));
                    inserir.setString(1, r.patrimonio());
                    inserir.setString(2, r.modelo());
                    inserir.setString(3, r.categoria());
                    inserir.setString(4, r.detalheEspecifico());
                    inserir.setString(5, r.status());
                    inserir.setString(6, r.dataAquisicao());
                    inserir.setString(7, r.ipEstatico());
                    inserir.setString(8, r.sistemaOperacional());
                    if (r.memoriaRamGb() == null) inserir.setNull(9, java.sql.Types.INTEGER);
                    else inserir.setInt(9, r.memoriaRamGb());
                    inserir.setString(10, r.departamento());
                    inserir.setString(11, r.responsavel());
                    if (r.numeroPortas() == null) inserir.setNull(12, java.sql.Types.INTEGER);
                    else inserir.setInt(12, r.numeroPortas());
                    if (inserir.executeUpdate() != 1)
                        throw new SQLException("Nao foi possivel inserir " + r.patrimonio());
                    inseridos++;
                }
                conn.commit();
            } catch (IOException | SQLException | RuntimeException e) {
                try { conn.rollback(); }
                catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return new ResultadoImportacao(inseridos, existentes);
    }

    public record ResultadoImportacao(int importados, int jaExistentes) {}

    private static String numero(Integer valor) { return valor == null ? null : Integer.toString(valor); }
    private static String texto(String valor) { return valor.isEmpty() ? null : valor; }
    private static Integer parseInteiro(String valor) { return valor.isBlank() ? null : Integer.valueOf(valor); }

    private static String escapar(String valor) {
        if (valor == null) return "";
        if (valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Quebras de linha dentro de campos nao sao suportadas.");
        }
        if (valor.contains(";") || valor.contains("\"") ) return "\"" + valor.replace("\"", "\"\"") + "\"";
        return valor;
    }

    /** Le linha de CSV separada por ponto e virgula com aspas duplas. */
    private static List<String> dividir(String linha) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean emAspas = false;
        boolean acabouAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (emAspas) {
                if (c == '\"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '\"') {
                        atual.append('\"'); i++;
                    } else { emAspas = false; acabouAspas = true; }
                } else { atual.append(c); }
            } else if (c == ';') {
                partes.add(atual.toString()); atual.setLength(0); acabouAspas = false;
            } else if (c == '\"' && atual.length() == 0 && !acabouAspas) {
                emAspas = true;
            } else if (acabouAspas || c == '\"') {
                throw new IllegalArgumentException("Aspas fora do lugar.");
            } else { atual.append(c); }
        }
        if (emAspas) throw new IllegalArgumentException("Aspas nao fechadas.");
        partes.add(atual.toString());
        return partes;
    }
}
