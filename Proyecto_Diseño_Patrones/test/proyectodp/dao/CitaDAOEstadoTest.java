package proyectodp.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import proyectodp.state.EstadoAtendida;
import proyectodp.state.EstadoCancelada;
import proyectodp.state.EstadoFinalizada;
import proyectodp.state.EstadoPendiente;
import proyectodp.state.EstadoReprogramada;

@DisplayName("DAO: mapeo de estados desde la base de datos")
class CitaDAOEstadoTest {

    @Test
    void mapeaCadaEstadoConocido() {
        assertInstanceOf(EstadoPendiente.class, CitaDAO.parseEstado("Pendiente"));
        assertInstanceOf(EstadoAtendida.class, CitaDAO.parseEstado("Atendida"));
        assertInstanceOf(EstadoReprogramada.class, CitaDAO.parseEstado("Reprogramada"));
        assertInstanceOf(EstadoCancelada.class, CitaDAO.parseEstado("Cancelada"));
        assertInstanceOf(EstadoFinalizada.class, CitaDAO.parseEstado("Finalizada"));
    }

    @Test
    void ignoraMayusculasEspaciosYValoresNulos() {
        assertInstanceOf(EstadoAtendida.class, CitaDAO.parseEstado("  atendida "));
        assertInstanceOf(EstadoPendiente.class, CitaDAO.parseEstado(null));
        assertInstanceOf(EstadoPendiente.class, CitaDAO.parseEstado("desconocido"));
    }
}
