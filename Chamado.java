public class Chamado {

    private int id;
    private String descricao;
    private int ativoId;

    public Chamado(String descricao, int ativoId) {
        this.descricao = descricao;
        this.ativoId = ativoId;
    }

    public Chamado(int id, String descricao, int ativoId) {
        this(descricao, ativoId);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getAtivoId() {
        return ativoId;
    }

    @Override
    public String toString() {
        return "Chamado ID: " + id
                + " | Descricao: " + descricao
                + " | Ativo ID: " + ativoId;
    }
}