package Techlab.dao;

/** Permite consultar chamados antigos sem inventar titulo, prioridade ou data. */
public record ChamadoResumo(int id, String descricao, int ativoId, String pendencias) {
    public boolean completo() { return pendencias == null || pendencias.isBlank(); }
}
