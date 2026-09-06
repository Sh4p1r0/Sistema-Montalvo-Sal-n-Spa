package proyectodp.proxy;

import proyectodp.facade.SistemaFacade;
import proyectodp.modelo.CitasMontalvo;
import java.util.List;

public class SistemaReal implements ISistemaAcceso {

    private final SistemaFacade facade;

    public SistemaReal() {
        this.facade = new SistemaFacade();
    }

    @Override
    public void registrarCita(String cliente, String hora, String tipoServicio) {
        facade.registrarCita(cliente, hora, tipoServicio);
    }

    @Override
    public List<CitasMontalvo> obtenerCitas() {
        return facade.obtenerCitas();
    }

    @Override
    public void atenderCita(int indice) {
        List<CitasMontalvo> lista = facade.obtenerCitas();
        if (indice >= 0 && indice < lista.size()) {
            lista.get(indice).atenderCita();
        }
    }

    @Override
    public void cancelarCita(int indice) {
        List<CitasMontalvo> lista = facade.obtenerCitas();
        if (indice >= 0 && indice < lista.size()) {
            lista.get(indice).cancelarCita();
        }
    }
}
