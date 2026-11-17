package Techlab.negocio;

import java.time.LocalDateTime;

/** Entidade do Service Desk; guarda o ID do ativo relacionado no banco. */
public final class ChamadoSuporte {
    private int id; // zero significa que ainda nao foi salvo
    private String titulo;
    private String descricao;
    private PrioridadeChamado prioridade;
    private LocalDateTime dataAbertura;
    private int ativoRelacionadoId;

    public ChamadoSuporte(String titulo, String descricao, PrioridadeChamado prioridade,
                         LocalDateTime dataAbertura, int ativoRelacionadoId) {
        setTitulo(titulo);
        setDescricao(descricao);
        setPrioridade(prioridade);
        setDataAbertura(dataAbertura);
        setAtivoRelacionadoId(ativoRelacionadoId);
    }

    public ChamadoSuporte(int id, String titulo, String descricao,
                         PrioridadeChamado prioridade, LocalDateTime dataAbertura,
                         int ativoRelacionadoId) {
        this(titulo, descricao, prioridade, dataAbertura, ativoRelacionadoId);
        setId(id);
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public PrioridadeChamado getPrioridade() { return prioridade; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public int getAtivoRelacionadoId() { return ativoRelacionadoId; }

    public void setId(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID do chamado deve ser positivo.");
        this.id = id;
    }

    public void setTitulo(String titulo) { this.titulo = exigirTexto(titulo, "Titulo"); }
    public void setDescricao(String descricao) { this.descricao = exigirTexto(descricao, "Descricao"); }
    public void setPrioridade(PrioridadeChamado prioridade) {
        if (prioridade == null) throw new IllegalArgumentException("Prioridade obrigatoria.");
        this.prioridade = prioridade;
    }
    public void setDataAbertura(LocalDateTime dataAbertura) {
        if (dataAbertura == null) throw new IllegalArgumentException("Data de abertura obrigatoria.");
        this.dataAbertura = dataAbertura;
    }
    public void setAtivoRelacionadoId(int ativoRelacionadoId) {
        if (ativoRelacionadoId <= 0) throw new IllegalArgumentException("ID de ativo relacionado deve ser positivo.");
        this.ativoRelacionadoId = ativoRelacionadoId;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode ser vazio.");
        return valor.trim();
    }

    @Override
    public String toString() {
        return "ChamadoSuporte [id=" + id + ", titulo='" + titulo
                + "', prioridade=" + prioridade + ", ativoId=" + ativoRelacionadoId + "]";
    }
}
