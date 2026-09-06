package proyectodp.servicios;

import proyectodp.interfaces.IServicioSalon;

public class ServicioManicura implements IServicioSalon {

    @Override
    public String obtenerDescripcion() {
        return "Manicura (Limpieza, Desinfección y Esmaltado)";
    }

    @Override
    public double obtenerPrecio() {
        return 25.00;
    }

}
