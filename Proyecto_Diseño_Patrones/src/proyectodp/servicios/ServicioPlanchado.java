package proyectodp.servicios;

import proyectodp.interfaces.IServicioSalon;

public class ServicioPlanchado implements IServicioSalon {

    @Override
    public String obtenerDescripcion() {
        return "Planchado y Tratamiento Capilar";
    }

    @Override
    public double obtenerPrecio() {
        return 15.00;
    }
}
