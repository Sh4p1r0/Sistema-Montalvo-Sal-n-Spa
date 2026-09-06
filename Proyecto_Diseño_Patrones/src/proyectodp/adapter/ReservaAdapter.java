package proyectodp.adapter;

import proyectodp.builder.CitaBuilder;
import proyectodp.factory.ServicioFactory;
import proyectodp.interfaces.IServicioSalon;
import proyectodp.modelo.CitasMontalvo;

public class ReservaAdapter {

    public CitasMontalvo convertir(ReservaExterna reserva) {

        IServicioSalon servicio = ServicioFactory.crearServicio(reserva.getServicio());

        return new CitaBuilder()
                .setCliente(reserva.getNombre())
                .setHora(reserva.getHorario())
                .setServicio(servicio.obtenerDescripcion())
                .setPrecio(servicio.obtenerPrecio())
                .build();
    }
}
