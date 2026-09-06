package proyectodp.proxy;

import proyectodp.modelo.CitasMontalvo;
import java.util.List;

public interface ISistemaAcceso {

    void registrarCita(String cliente, String hora, String tipoServicio);

    List<CitasMontalvo> obtenerCitas();

    void atenderCita(int indice);

    void cancelarCita(int indice);
}
