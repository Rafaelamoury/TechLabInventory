package Techlab.negocio;

import java.time.LocalDateTime;

/** Classe abstrata de equipamentos, adaptada do exemplo do professor. */
public abstract class AtivoTI {
    // O SQLite atribui o ID ao salvar; zero significa "ainda nao persistido".
    private int id;
    private String codigoPatrimonio;
    private String modelo;
    private String status;
    private LocalDateTime dataAquisicao;

    protected AtivoTI(String codigoPatrimonio, String modelo, String status,
                      LocalDateTime dataAquisicao) {
        setCodigoPatrimonio(codigoPatrimonio);
        setModelo(modelo);
        setStatus(status);
        setDataAquisicao(dataAquisicao);
    }

    protected AtivoTI(int id, String codigoPatrimonio, String modelo,
                      LocalDateTime dataAquisicao, String status) {
        this(codigoPatrimonio, modelo, status, dataAquisicao);
        setId(id);
    }

    public int getId() { return this.id; }
    public String getCodigoPatrimonio() { return this.codigoPatrimonio; }
    public String getModelo() { return this.modelo; }
    public String getStatus() { return this.status; }
    public LocalDateTime getDataAquisicao() { return this.dataAquisicao; }

    // Alias temporarios para compatibilidade com o codigo inicial do professor.
    public int getid() { return getId(); }
    public String getcodigoPatrimonio() { return getCodigoPatrimonio(); }
    public String getmodelo() { return getModelo(); }
    public String getstatus() { return getStatus(); }
    public LocalDateTime getdataAquisicao() { return getDataAquisicao(); }

    public void setId(int id) {
        if (id <= 0) throw new IllegalArgumentException("O ID deve ser positivo.");
        this.id = id;
    }
    public void setCodigoPatrimonio(String codigoPatrimonio) {
        this.codigoPatrimonio = exigirTexto(codigoPatrimonio, "Patrimonio");
    }
    public void setModelo(String modelo) {
        this.modelo = exigirTexto(modelo, "Modelo");
    }
    public void setStatus(String status) {
        this.status = exigirTexto(status, "Status");
    }
    public void setDataAquisicao(LocalDateTime dataAquisicao) {
        if (dataAquisicao == null) {
            throw new IllegalArgumentException("Data de aquisicao e obrigatoria.");
        }
        this.dataAquisicao = dataAquisicao;
    }

    public void setid(int id) { setId(id); }
    public void setcodigoPatrimonio(String valor) { setCodigoPatrimonio(valor); }
    public void setmodelo(String valor) { setModelo(valor); }
    public void setstatus(String valor) { setStatus(valor); }
    public void setdataAquisicao(LocalDateTime valor) { setDataAquisicao(valor); }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " nao pode estar vazio.");
        }
        return valor.trim();
    }

    public abstract double calcularCustoManutencaoMensal();

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + id
                + ", patrimonio=" + codigoPatrimonio
                + ", modelo=" + modelo + ", status=" + status + "]";
    }
}
