package proyectodp.servicios;

import proyectodp.interfaces.IServicioSalon;

public class ServicioCorte implements IServicioSalon {

    @Override
    public String obtenerDescripcion() {
        return "Corte de Cabello (Incluye lavado y secado)";
    }

    @Override
    public double obtenerPrecio() {
        return 20.00;
    }

}
