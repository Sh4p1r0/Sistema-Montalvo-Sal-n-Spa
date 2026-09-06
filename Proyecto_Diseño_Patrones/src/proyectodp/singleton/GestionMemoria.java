package proyectodp.singleton;

import java.util.ArrayList;
import java.util.List;
import proyectodp.interfaces.IGestionCitas;
import proyectodp.modelo.CitasMontalvo;

public class GestionMemoria implements IGestionCitas {

    private static GestionMemoria instancia;
    private final List<CitasMontalvo> listaCitas;

    private GestionMemoria() {
        this.listaCitas = new ArrayList<>();
    }

    public static synchronized GestionMemoria getInstancia() {
        if (instancia == null) {
            instancia = new GestionMemoria();
        }
        return instancia;
    }

    @Override
    public void agregarCita(CitasMontalvo cita) {
        listaCitas.add(cita);
    }

    @Override
    public List<CitasMontalvo> listarCitas() {
        return new ArrayList<>(listaCitas); 
    }

    @Override
    public CitasMontalvo buscarPorId(String id) {
        for (CitasMontalvo c : listaCitas) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public boolean eliminarCita(String id) {
        return listaCitas.removeIf(c -> c.getId().equalsIgnoreCase(id));
    }
}
