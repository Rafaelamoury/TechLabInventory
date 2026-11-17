package Techlab.negocio;

import java.time.LocalDateTime;

/** Subclasse requerida pelo DAO do professor, ainda ausente no repositorio original. */
public class EstacaoTrabalho extends AtivoTI {
    private String departamento;
    private String responsavel;

    public EstacaoTrabalho(String patrimonio, String modelo, String status,
                          LocalDateTime dataAquisicao, String departamento, String responsavel) {
        super(patrimonio, modelo, status, dataAquisicao);
        setDepartamento(departamento);
        setResponsavel(responsavel);
    }

    public EstacaoTrabalho(int id, String patrimonio, String modelo,
                          LocalDateTime dataAquisicao, String status,
                          String departamento, String responsavel) {
        super(id, patrimonio, modelo, dataAquisicao, status);
        setDepartamento(departamento);
        setResponsavel(responsavel);
    }

    public String getDepartamento() { return departamento; }
    public String getResponsavel() { return responsavel; }
    public void setDepartamento(String departamento) {
        if (departamento == null || departamento.isBlank())
            throw new IllegalArgumentException("Departamento nao pode estar vazio.");
        this.departamento = departamento.trim();
    }
    public void setResponsavel(String responsavel) {
        if (responsavel == null || responsavel.isBlank())
            throw new IllegalArgumentException("Responsavel nao pode estar vazio.");
        this.responsavel = responsavel.trim();
    }

    @Override
    public double calcularCustoManutencaoMensal() {
        return 80.0; // Criterio ilustrativo; confirmar com professor.
    }
}
