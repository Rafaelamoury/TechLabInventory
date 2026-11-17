package Techlab.negocio;

import java.time.LocalDateTime;

/** Subclasse requerida pelo DAO do professor, ainda ausente no repositorio original. */
public class Servidor extends AtivoTI {
    private String ipEstatico;
    private String sistemaOperacional;
    private int memoriaRamGb;

    public Servidor(String patrimonio, String modelo, String status,
                   LocalDateTime dataAquisicao, String ipEstatico,
                   String sistemaOperacional, int memoriaRamGb) {
        super(patrimonio, modelo, status, dataAquisicao);
        setIpEstatico(ipEstatico);
        setSistemaOperacional(sistemaOperacional);
        setMemoriaRamGb(memoriaRamGb);
    }

    public Servidor(int id, String patrimonio, String modelo,
                   LocalDateTime dataAquisicao, String status,
                   String ipEstatico, String sistemaOperacional, int memoriaRamGb) {
        super(id, patrimonio, modelo, dataAquisicao, status);
        setIpEstatico(ipEstatico);
        setSistemaOperacional(sistemaOperacional);
        setMemoriaRamGb(memoriaRamGb);
    }

    public String getIpEstatico() { return ipEstatico; }
    public String getSistemaOperacional() { return sistemaOperacional; }
    public int getMemoriaRamGb() { return memoriaRamGb; }
    public void setIpEstatico(String ipEstatico) {
        if (ipEstatico == null || ipEstatico.isBlank())
            throw new IllegalArgumentException("IP nao pode estar vazio.");
        this.ipEstatico = ipEstatico.trim();
    }
    public void setSistemaOperacional(String sistemaOperacional) {
        if (sistemaOperacional == null || sistemaOperacional.isBlank())
            throw new IllegalArgumentException("Sistema operacional nao pode estar vazio.");
        this.sistemaOperacional = sistemaOperacional.trim();
    }
    public void setMemoriaRamGb(int memoriaRamGb) {
        if (memoriaRamGb <= 0)
            throw new IllegalArgumentException("Memoria RAM deve ser positiva.");
        this.memoriaRamGb = memoriaRamGb;
    }
    @Override
    public double calcularCustoManutencaoMensal() {
        return 100.0 + memoriaRamGb * 5.0; // Criterio ilustrativo; confirmar com professor.
    }
}
