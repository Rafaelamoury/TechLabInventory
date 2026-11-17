package Techlab.service;

import Techlab.dao.AtivoDAO;
import Techlab.dao.AtivoResumo;
import Techlab.dao.ChamadoDAO;
import Techlab.dao.ChamadoResumo;
import Techlab.interfaces.IConexaoBD;
import Techlab.negocio.PrioridadeChamado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** Completa apenas campos faltantes, sem alterar IDs ou atributos conhecidos. */
public final class ComplementacaoLegadosService {
    private static final Set<String> CAMPOS_ATIVOS = Set.of(
            "status", "data_aquisicao", "ip_estatico", "sistema_operacional",
            "memoria_ram_gb", "departamento", "responsavel", "numero_portas");
    private static final Set<String> CAMPOS_CHAMADOS = Set.of("titulo", "prioridade", "data_abertura");
    private final IConexaoBD banco;
    private final AtivoDAO ativos;
    private final ChamadoDAO chamados;

    public ComplementacaoLegadosService(IConexaoBD banco) {
        if (banco == null) throw new IllegalArgumentException("Conexao obrigatoria.");
        this.banco = banco;
        this.ativos = new AtivoDAO(banco);
        this.chamados = new ChamadoDAO(banco);
    }

    public List<String> camposPendentesAtivo(int id) throws SQLException {
        for (AtivoResumo r : ativos.listarResumos()) {
            if (r.id() == id) {
                if (!Set.of("Servidor", "EstacaoTrabalho", "DispositivoRede").contains(r.categoria())) {
                    throw new IllegalStateException("Categoria nao reconhecida para ativo " + id);
                }
                return extrairCampos(r.pendencias(), CAMPOS_ATIVOS);
            }
        }
        throw new IllegalArgumentException("Equipamento nao encontrado: " + id);
    }

    public List<String> camposPendentesChamado(int id) throws SQLException {
        for (ChamadoResumo r : chamados.listarResumos()) {
            if (r.id() == id) return extrairCampos(r.pendencias(), CAMPOS_CHAMADOS);
        }
        throw new IllegalArgumentException("Chamado nao encontrado: " + id);
    }

    public void completarAtivo(int id, Map<String, String> dados) throws SQLException {
        completar("ativos", id, camposPendentesAtivo(id), dados, CAMPOS_ATIVOS);
        if (!camposPendentesAtivo(id).isEmpty() || ativos.buscarPorId(id) == null) {
            throw new SQLException("Falhou a verificacao do equipamento apos complementacao.");
        }
    }

    public void completarChamado(int id, Map<String, String> dados) throws SQLException {
        completar("chamados", id, camposPendentesChamado(id), dados, CAMPOS_CHAMADOS);
        if (!camposPendentesChamado(id).isEmpty() || chamados.buscarPorId(id) == null) {
            throw new SQLException("Falhou a verificacao do chamado apos complementacao.");
        }
    }

    private void completar(String tabela, int id, List<String> pendentes,
                           Map<String, String> dados, Set<String> permitidos) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo.");
        if (pendentes.isEmpty()) throw new IllegalStateException("Cadastro ja completo.");
        if (dados == null || !dados.keySet().equals(new LinkedHashSet<>(pendentes))) {
            throw new IllegalArgumentException("Informe todos e somente os campos pendentes: " + pendentes);
        }
        Map<String, Object> preparados = new LinkedHashMap<>();
        for (String campo : pendentes) {
            if (!permitidos.contains(campo)) throw new IllegalArgumentException("Campo nao permitido: " + campo);
            preparados.put(campo, validar(campo, dados.get(campo)));
        }
        StringJoiner camposSql = new StringJoiner(", ");
        for (String campo : preparados.keySet()) camposSql.add(campo + " = ?");
        // O nome da tabela e todos os nomes das colunas sao validados contra constantes locais.
        String sql = "UPDATE " + tabela + " SET " + camposSql + " WHERE id = ?";
        try (Connection conexao = banco.obterConexao()) {
            conexao.setAutoCommit(false);
            try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
                int i = 1;
                for (Object valor : preparados.values()) {
                    if (valor instanceof Integer numero) stmt.setInt(i++, numero);
                    else stmt.setString(i++, (String) valor);
                }
                stmt.setInt(i, id);
                if (stmt.executeUpdate() != 1) throw new SQLException("Registro nao atualizado: " + id);
                conexao.commit();
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    private static List<String> extrairCampos(String pendencias, Set<String> permitidos) {
        List<String> campos = new ArrayList<>();
        if (pendencias == null || pendencias.isBlank()) return campos;
        for (String p : pendencias.split(",")) {
            String nome = p.trim().replaceAll("\\s*\\(.*", "");
            if (!permitidos.contains(nome)) {
                throw new IllegalStateException("Pendencia nao tratada automaticamente: " + p.trim());
            }
            if (!campos.contains(nome)) campos.add(nome);
        }
        return campos;
    }

    private static Object validar(String campo, String valor) {
        if (valor == null || valor.isBlank())
            throw new IllegalArgumentException("Informe um valor confirmado para " + campo);
        valor = valor.trim();
        return switch (campo) {
            case "data_aquisicao" -> {
                try { yield LocalDate.parse(valor).atStartOfDay().toString(); }
                catch (DateTimeParseException e) { throw new IllegalArgumentException("Use AAAA-MM-DD para aquisicao.", e); }
            }
            case "data_abertura" -> {
                try { yield LocalDateTime.parse(valor).toString(); }
                catch (DateTimeParseException e) { throw new IllegalArgumentException("Use AAAA-MM-DDTHH:MM para abertura.", e); }
            }
            case "prioridade" -> {
                try { yield PrioridadeChamado.valueOf(valor.toUpperCase(Locale.ROOT)).name(); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("Prioridade deve ser BAIXA, MEDIA, ALTA ou CRITICA.", e); }
            }
            case "memoria_ram_gb", "numero_portas" -> {
                try {
                    int numero = Integer.parseInt(valor);
                    if (numero <= 0) throw new IllegalArgumentException(campo + " deve ser maior que zero.");
                    yield numero;
                } catch (NumberFormatException e) { throw new IllegalArgumentException(campo + " deve ser inteiro positivo.", e); }
            }
            default -> valor;
        };
    }
}
