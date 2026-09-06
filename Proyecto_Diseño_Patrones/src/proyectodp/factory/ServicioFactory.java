package proyectodp.factory;

import proyectodp.servicios.ServicioAsesoria;
import proyectodp.servicios.ServicioPlanchado;
import proyectodp.servicios.ServicioManicura;
import proyectodp.servicios.ServicioMasaje;
import proyectodp.servicios.ServicioCorte;
import proyectodp.interfaces.IServicioSalon;

public class ServicioFactory {

    public static IServicioSalon crearServicio(String tipo_servi) {
        if (tipo_servi == null || tipo_servi.isEmpty()) {
            throw new IllegalArgumentException("Tipo de servicio vacío");
        }
        switch (tipo_servi.toUpperCase()) {
            case "CORTE":
                return new ServicioCorte();
            case "PLANCHADO":
                return new ServicioPlanchado();
            case "MANICURA":
                return new ServicioManicura();
            case "ASESORIA":
                return new ServicioAsesoria();
            case "MASAJE":
                return new ServicioMasaje();
            default:
                throw new IllegalArgumentException("El servicio solicitado no existe " + tipo_servi);
        }
    }

}
