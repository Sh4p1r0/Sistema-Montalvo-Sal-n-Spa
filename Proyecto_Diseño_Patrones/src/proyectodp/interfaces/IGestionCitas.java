package proyectodp.interfaces;

import java.util.List;
import proyectodp.modelo.CitasMontalvo;

public interface IGestionCitas {

    void agregarCita(CitasMontalvo cita);

    List<CitasMontalvo> listarCitas();

    CitasMontalvo buscarPorId(String id);

    boolean eliminarCita(String id);
}
