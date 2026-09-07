# Montalvo Salón & Spa — Sistema de Gestión de Citas & Caja Web

> Sistema comercial de gestión de citas, recepción, arqueo de caja y agenda en tiempo real para salón de belleza y spa, implementado con **Arquitectura de Patrones de Diseño (GoF)** en Java, persistencia resiliente en **PostgreSQL (Supabase)** y un frontend moderno con **Sistema de Diseño Revolut** (alto contraste Stark Black y Canvas Light, botones pill 9999px, tipografía Inter y Bento Grid interactivo).

---

## Características Principales

- **Sistema de Diseño Revolut:** Interfaz de alto impacto basada en lienzos limpios (Canvas Light), cabecera Stark Black (#000000), botones píldora redondeados a 9999px y escala tipográfica Inter.
- **Agenda Activa en Tiempo Real:** Visualización compacta de disponibilidad horaria estilo teclado inteligente (horarios libres en verde esmeralda y ocupados con detalle de estilista).
- **Tarjetas Bento de Citas:** Citas organizadas con bordes hairline de 1px (#e2e2e7), tipografía Inter jerarquizada y micro-elevación al interactuar.
- **Píldoras de Estado Armónicas:** Indicadores de estado en tiempo real (Pendiente en naranja, En Atención en verde esmeralda, Reprogramada en azul zafiro y Cancelada en rojo).
- **Control y Arqueo de Caja Diario/Semanal:** Módulo de balance y auditoría en tiempo real con desglose de facturación por especialista, cálculo de ticket promedio y filtros temporales rápidos (Ayer, Hoy, Esta Semana, Mañana, Todas).
- **Transiciones y Micro-Interacciones:** Animación fluida de desvanecimiento (400ms) al finalizar citas antes de su archivado automático en caja.
- **Doble Persistencia Resiliente:** Conexión nativa a base de datos en la nube (Supabase PostgreSQL) con sincronización automática y fallback en memoria.

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
- **Base de Datos:** PostgreSQL en la nube (Supabase) con driver JDBC.
- **Frontend:** HTML5 Semántico, CSS3 Vanilla Moderno (Variables :root, Flexbox, CSS Grid), JavaScript Moderno (ES6+).
- **Diseño UI/UX:** Inspirado en el Sistema de Diseño Revolut, Stark Black (#000000), Canvas Light (#ffffff), Ink (#191c1f), verde esmeralda (#00a87e) y Bento Grid interactivo.

---

## Instrucciones de Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Sh4p1r0/Sistema-Montalvo-Sal-n-Spa.git
   cd Sistema-Montalvo-Sal-n-Spa
   ```

2. Ejecutar la aplicación Java:
   - Desde tu IDE favorito (VS Code, IntelliJ, Eclipse, NetBeans) abriendo el proyecto y ejecutando con un clic la clase principal:
     `proyectodp.main.MainWeb` (o `proyectodp.main.Main`).
   - O por línea de comandos:
     ```bash
     java -cp "build;lib/*;Proyecto_Diseño_Patrones/lib/*" proyectodp.main.MainWeb
     ```

3. Abrir en el navegador:
   ```text
   http://localhost:8080
   ```

---

## Proyecto
Proyecto final desarrollado para la carrera de Ingeniería de Sistemas e Informática - Universidad Tecnológica del Perú (UTP).
