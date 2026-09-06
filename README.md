# Montalvo Salón & Spa — Sistema de Gestión de Citas & Caja Web

> Sistema comercial de gestión de citas, recepción, arqueo de caja y agenda en tiempo real para salón de belleza y spa, implementado con **Arquitectura de Patrones de Diseño (GoF)** en Java, persistencia resiliente en **PostgreSQL (Supabase)** y un frontend moderno con diseño **Dark Luxury & Bento Grid**.

---

## Características Principales

- **Agenda Activa en Tiempo Real:** Visualización compacta de disponibilidad horaria estilo teclado inteligente (libre en verde esmeralda neón y ocupado en rojo).
- **Tarjetas Bento Glassmorphism:** Citas con desenfoque de fondo (`backdrop-filter: blur(12px)`), bordes dorados luminosos y tipografía *Outfit* / *Plus Jakarta Sans*.
- **Píldoras de Estado Pulsantes:** Animaciones en vivo (*Pending*, *Active / En Atención*, *Cancelled*).
- **Control y Arqueo de Caja Diario/Semanal:** Cálculo automático de ingresos al cobrar citas con filtros rápidos (**"Ayer"**, **"Hoy"**, **"Esta Semana"**) y reporte de cierre imprimible.
- **Transiciones y Micro-Interacciones:** Animación fluida de desvanecimiento (400ms) al finalizar citas antes de su archivado.
- **Doble Persistencia:** Conexión a base de datos en la nube (Supabase PostgreSQL) con fallback automático y resiliente en memoria.

---

## Arquitectura de Patrones de Diseño (GoF)

El núcleo del sistema en Java implementa patrones de diseño reconocidos:

| Patrón GoF | Tipo | Aplicación en el Proyecto |
| :--- | :--- | :--- |
| **Facade** | Estructural | `SistemaFacade`: Unifica la lógica de negocio, reportes y caja para el frontend web. |
| **State** | Comportamiento | `EstadoPendiente`, `EstadoAtendida`, `EstadoReprogramada`, `EstadoFinalizada`, `EstadoCancelada`. |
| **Builder** | Creacional | `CitaBuilder`: Construcción paso a paso y validada de citas. |
| **Factory Method** | Creacional | `ServicioFactory`: Creación de servicios específicos del salón. |
| **Singleton** | Creacional | `ConexionDB` y `GestionMemoria`: Acceso único al pool de conexiones y caché de datos. |
| **Proxy** | Estructural | Control de acceso y seguridad para roles de usuario y recepcionista. |
| **DAO** | Arquitectura | `CitaDAO`, `UsuarioDAO`: Abstracción completa de persistencia en PostgreSQL. |

---

## Tecnologías

- **Backend:** Java 17+ (Servidor HTTP nativo de alto rendimiento, sin frameworks pesados).
- **Base de Datos:** PostgreSQL en la nube (Supabase).
- **Frontend:** HTML5 Semántico, CSS3 Vanilla Moderno (Variables `:root`, Glassmorphism, CSS Grid), JavaScript Moderno.
- **Diseño UI/UX:** Estética Dark Luxury, acentos dorados `#d4af37`, verde esmeralda y Bento Grid asimétrico.

---

## Instrucciones de Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Sh4p1r0/Sistema-Montalvo-Sal-n-Spa.git
   cd Sistema-Montalvo-Sal-n-Spa
   ```
2. Ejecutar la aplicación Java:
   * Desde tu IDE favorito (Eclipse, IntelliJ, VS Code, NetBeans) ejecutando la clase principal:
     `proyectodp.main.Main` (o `proyectodp.main.MainWeb`).
   * O por línea de comandos:
     ```bash
     java -cp "Proyecto_Diseño_Patrones/bin;Proyecto_Diseño_Patrones/lib/*" proyectodp.main.MainWeb
     ```
3. Abrir en el navegador:
   ```text
   http://localhost:8080
   ```

---

## Desarrolladores
Proyecto final desarrollado para la carrera de Ingeniería de Sistemas e Informática - Universidad Tecnológica del Perú-Piura
