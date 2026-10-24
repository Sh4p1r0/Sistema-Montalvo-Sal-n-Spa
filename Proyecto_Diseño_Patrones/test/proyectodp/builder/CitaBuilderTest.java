package proyectodp.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.modelo.CitasMontalvo;
import proyectodp.state.EstadoAtendida;

@DisplayName("Patrón Builder: construcción de citas")
class CitaBuilderTest {

    @Test
    void aplicaValoresPorDefectoCuandoFaltanDatos() {
        CitasMontalvo cita = new CitaBuilder().setCliente("Ana").build();

        assertEquals("Pendiente", cita.getEstado().obtenerNombreEstado());
        assertEquals("Especialista Montalvo", cita.getEstilista());
        assertEquals(45, cita.getDuracionMinutos());
        assertEquals(LocalDate.now().toString(), cita.getFecha());
        assertNotNull(cita.getId());
        assertEquals(8, cita.getId().length());
    }

    @Test
    void respetaTodosLosValoresIndicados() {
        CitasMontalvo cita = new CitaBuilder()
                .setId("abc12345")
                .setCliente("Ana Torres")
                .setFecha("2026-11-10")
                .setHora("10:00")
                .setServicio("Masaje Relajante")
                .setPrecio(80.5)
                .setEstilista("Carlos Mendoza")
                .setDuracionMinutos(60)
                .setEstado(new EstadoAtendida())
                .build();

        assertEquals("abc12345", cita.getId());
        assertEquals("Ana Torres", cita.getNombreCliente());
        assertEquals("2026-11-10", cita.getFecha());
        assertEquals("10:00", cita.getHora());
        assertEquals("Masaje Relajante", cita.getTipoServicio());
        assertEquals(80.5, cita.getPrecio(), 0.001);
        assertEquals("Carlos Mendoza", cita.getEstilista());
        assertEquals(60, cita.getDuracionMinutos());
        assertEquals("Atendida", cita.getEstado().obtenerNombreEstado());
    }

    @Test
    void permiteEncadenarYReutilizarElBuilder() {
        CitaBuilder builder = new CitaBuilder().setEstilista("Carlos Mendoza");
        CitasMontalvo primera = builder.setCliente("Ana").build();
        CitasMontalvo segunda = builder.setCliente("Luis").build();

        assertEquals("Ana", primera.getNombreCliente());
        assertEquals("Luis", segunda.getNombreCliente());
        assertNotEquals(primera.getId(), segunda.getId());
    }
}
