package proyectodp.servicios;

import proyectodp.interfaces.IServicioSalon;

public class ServicioAsesoria implements IServicioSalon {

    @Override
    public String obtenerDescripcion() {
        return "Asesoría de Imagen y Cambio de Look";
    }

    @Override
    public double obtenerPrecio() {
        return 50.00;
    }

}
