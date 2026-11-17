package Techlab.negocio;

import java.time.LocalDateTime;

/** Implementacao inicial do professor, com validacao e sobrecarga para leitura do banco. */
public class DispositivoRede extends AtivoTI {
    private int numeroPortas;

    public DispositivoRede(String patrimonio, String modelo, String status,
                          LocalDateTime dataAquisicao, int numeroPortas) {
        super(patrimonio, modelo, status, dataAquisicao);
        setNumeroPortas(numeroPortas);
    }

    public DispositivoRede(int id, String patrimonio, String modelo,
                          LocalDateTime dataAquisicao, String status,
                          int numeroPortas) {
        super(id, patrimonio, modelo, dataAquisicao, status);
        setNumeroPortas(numeroPortas);
    }

    public int getNumeroPortas() { return numeroPortas; }
    public void setNumeroPortas(int numeroPortas) {
        if (numeroPortas <= 0) {
            throw new IllegalArgumentException("Numero de portas deve ser positivo.");
        }
        this.numeroPortas = numeroPortas;
    }

    @Override
    public double calcularCustoManutencaoMensal() {
        return numeroPortas * 200.0; // Formula presente no codigo do professor.
    }
}
