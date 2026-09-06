package proyectodp.servicios;

import proyectodp.interfaces.IServicioSalon;

public class ServicioMasaje implements IServicioSalon {

    @Override
    public String obtenerDescripcion() {
        return "Masaje Relajante";
    }

    @Override
    public double obtenerPrecio() {
        return 35.00;

    }
}
