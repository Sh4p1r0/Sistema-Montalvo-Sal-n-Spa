package proyectodp.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.interfaces.IServicioSalon;
import proyectodp.servicios.ServicioAsesoria;
import proyectodp.servicios.ServicioCorte;
import proyectodp.servicios.ServicioManicura;
import proyectodp.servicios.ServicioMasaje;
import proyectodp.servicios.ServicioPlanchado;

@DisplayName("Patrón Factory: creación de servicios")
class ServicioFactoryTest {

    @Test
    void creaCadaTipoDeServicio() {
        assertInstanceOf(ServicioCorte.class, ServicioFactory.crearServicio("CORTE"));
        assertInstanceOf(ServicioPlanchado.class, ServicioFactory.crearServicio("PLANCHADO"));
        assertInstanceOf(ServicioManicura.class, ServicioFactory.crearServicio("MANICURA"));
        assertInstanceOf(ServicioAsesoria.class, ServicioFactory.crearServicio("ASESORIA"));
        assertInstanceOf(ServicioMasaje.class, ServicioFactory.crearServicio("MASAJE"));
    }

    @Test
    void ignoraMayusculasYMinusculas() {
        assertInstanceOf(ServicioCorte.class, ServicioFactory.crearServicio("corte"));
        assertInstanceOf(ServicioMasaje.class, ServicioFactory.crearServicio("MaSaJe"));
    }

    @Test
    void todoServicioTieneDescripcionYPrecioPositivo() {
        for (String tipo : new String[]{"CORTE", "PLANCHADO", "MANICURA", "ASESORIA", "MASAJE"}) {
            IServicioSalon servicio = ServicioFactory.crearServicio(tipo);
            assertNotNull(servicio.obtenerDescripcion());
            assertFalse(servicio.obtenerDescripcion().isBlank(), "Descripción vacía para " + tipo);
            assertTrue(servicio.obtenerPrecio() > 0, "Precio inválido para " + tipo);
        }
    }

    @Test
    void rechazaTipoNuloOVacio() {
        assertThrows(IllegalArgumentException.class, () -> ServicioFactory.crearServicio(null));
        assertThrows(IllegalArgumentException.class, () -> ServicioFactory.crearServicio(""));
    }

    @Test
    void rechazaServicioInexistente() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ServicioFactory.crearServicio("TATUAJE"));
        assertTrue(ex.getMessage().contains("TATUAJE"));
    }
}
