package Techlab.dao;

/** Resumo dos registros de banco, inclusive os legados com dados faltantes. */
public record AtivoResumo(int id, String patrimonio, String modelo,
                          String categoria, String pendencias) {
    public boolean completo() {
        return pendencias == null || pendencias.isBlank();
    }
}
