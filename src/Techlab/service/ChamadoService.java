package Techlab.service;

import Techlab.interfaces.INotificavel;
import Techlab.interfaces.IPersistencia;
import Techlab.negocio.ChamadoSuporte;

import java.sql.SQLException;
import java.util.Objects;

/** Coordena a persistencia do chamado e a notificacao apos gravacao bem-sucedida. */
public final class ChamadoService {
    private final IPersistencia<ChamadoSuporte> persistencia;
    private final INotificavel notificador;

    public ChamadoService(IPersistencia<ChamadoSuporte> persistencia, INotificavel notificador) {
        this.persistencia = Objects.requireNonNull(persistencia, "Persistencia obrigatoria.");
        this.notificador = Objects.requireNonNull(notificador, "Notificador obrigatorio.");
    }

    public void abrir(ChamadoSuporte chamado) throws SQLException {
        persistencia.salvar(chamado);
        notificador.notificar("Chamado #" + chamado.getId()
                + " aberto para equipamento #" + chamado.getAtivoRelacionadoId()
                + " (" + chamado.getPrioridade() + ").");
    }

    public void atualizar(ChamadoSuporte chamado) throws SQLException {
        persistencia.atualizar(chamado);
        notificador.notificar("Chamado #" + chamado.getId()
                + " atualizado (" + chamado.getPrioridade() + ").");
    }
}
