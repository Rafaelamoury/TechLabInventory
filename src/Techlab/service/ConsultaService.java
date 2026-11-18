package Techlab.service;

import Techlab.dao.AtivoResumo;
import Techlab.dao.ChamadoResumo;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Filtra e ordena DTOs em memoria usando colecoes genericas e Streams. */
public final class ConsultaService {
    private ConsultaService() { }

    public static List<AtivoResumo> filtrarEOrdenarAtivos(List<AtivoResumo> originais, String termo) {
        Objects.requireNonNull(originais, "Colecao de equipamentos obrigatoria.");
        String busca = normalizar(termo);
        return originais.stream()
                .filter(a -> contem(a.patrimonio(), busca)
                        || contem(a.modelo(), busca) || contem(a.categoria(), busca))
                .sorted(Comparator.comparing(AtivoResumo::patrimonio, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(AtivoResumo::id))
                .toList();
    }

    public static List<ChamadoResumo> filtrarEOrdenarChamados(List<ChamadoResumo> originais, String termo) {
        Objects.requireNonNull(originais, "Colecao de chamados obrigatoria.");
        String busca = normalizar(termo);
        return originais.stream()
                .filter(c -> contem(c.descricao(), busca))
                .sorted(Comparator.comparing(ChamadoResumo::descricao, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(ChamadoResumo::id))
                .toList();
    }

    private static boolean contem(String valor, String busca) {
        return valor != null && normalizar(valor).contains(busca);
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
