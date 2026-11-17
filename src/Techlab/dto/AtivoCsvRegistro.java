package Techlab.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fotografia de um ativo para exportacao/importacao.
 * Permite representar os registros antigos com campos ainda desconhecidos,
 * sem inventar dados de dominio (como a data de aquisicao).
 */
public record AtivoCsvRegistro(
        int id, String patrimonio, String modelo, String categoria,
        String detalheEspecifico, String status, String dataAquisicao,
        String ipEstatico, String sistemaOperacional, Integer memoriaRamGb,
        String departamento, String responsavel, Integer numeroPortas) {

    public AtivoCsvRegistro {
        if (id <= 0) throw new IllegalArgumentException("ID de origem deve ser positivo.");
        if (vazio(patrimonio)) throw new IllegalArgumentException("Patrimonio nao pode ficar vazio.");
        if (vazio(modelo)) throw new IllegalArgumentException("Modelo nao pode ficar vazio.");
        if (!"Servidor".equals(categoria) && !"EstacaoTrabalho".equals(categoria)
                && !"DispositivoRede".equals(categoria)) {
            throw new IllegalArgumentException("Categoria desconhecida: " + categoria);
        }
        if (!vazio(dataAquisicao)) {
            try { LocalDateTime.parse(dataAquisicao); }
            catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Data de aquisicao invalida: " + dataAquisicao, e);
            }
        }
        if (memoriaRamGb != null && memoriaRamGb <= 0) {
            throw new IllegalArgumentException("RAM precisa ser positiva.");
        }
        if (numeroPortas != null && numeroPortas <= 0) {
            throw new IllegalArgumentException("Numero de portas precisa ser positivo.");
        }
    }

    public List<String> pendencias() {
        List<String> pendencias = new ArrayList<>();
        if (vazio(status)) pendencias.add("status");
        if (vazio(dataAquisicao)) pendencias.add("data_aquisicao");
        switch (categoria) {
            case "Servidor" -> {
                if (vazio(ipEstatico)) pendencias.add("ip_estatico");
                if (vazio(sistemaOperacional)) pendencias.add("sistema_operacional");
                if (memoriaRamGb == null) pendencias.add("memoria_ram_gb");
            }
            case "EstacaoTrabalho" -> {
                if (vazio(departamento)) pendencias.add("departamento");
                if (vazio(responsavel)) pendencias.add("responsavel");
            }
            case "DispositivoRede" -> {
                if (numeroPortas == null) pendencias.add("numero_portas");
            }
            default -> throw new IllegalStateException("Categoria nao reconhecida.");
        }
        return List.copyOf(pendencias);
    }

    public boolean completo() { return pendencias().isEmpty(); }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
