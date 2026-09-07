# Montalvo Salón & Spa - Sistema de Gestión de Citas y Caja Web

Sistema integral de gestión de citas, recepción, arqueo de caja y agenda en tiempo real para salón de belleza y spa. Desarrollado con una arquitectura orientada a Patrones de Diseño (GoF) en Java, persistencia conectada a PostgreSQL (Supabase) y una interfaz web construida bajo los principios del Sistema de Diseño Revolut.

---

## Arquitectura y Patrones de Diseño (GoF)

El núcleo del sistema en Java implementa patrones de diseño que aseguran modularidad, escalabilidad y bajo acoplamiento:

| Patrón GoF | Clasificación | Componente y Aplicación |
| :--- | :--- | :--- |
| **Facade** | Estructural | `SistemaFacade`: Centraliza y simplifica las operaciones de citas, servicios, especialistas y caja para los controladores web. |
| **State** | Comportamiento | `EstadoCita` (`EstadoPendiente`, `EstadoAtendida`, `EstadoReprogramada`, `EstadoFinalizada`, `EstadoCancelada`): Modela el ciclo de vida de cada cita y sus reglas de transición. |
| **Builder** | Creacional | `CitaBuilder`: Construcción paso a paso de citas garantizando validaciones de negocio, identificadores únicos y consistencia de datos. |
| **Factory Method** | Creacional | `ServicioFactory`: Instanciación dinámica del catálogo de servicios según su categoría y área técnica. |
| **Singleton** | Creacional | `ConexionDB` y `GestionMemoria`: Control centralizado y único del pool de conexión a la base de datos y la memoria operativa. |
| **Proxy** | Estructural | `SistemaProxy`: Control de acceso, seguridad y auditoría de peticiones para usuarios y recepcionistas. |
| **DAO** | Arquitectura | `CitaDAO` y `UsuarioDAO`: Capa de persistencia desacoplada para consultas y operaciones en PostgreSQL. |

---

## Módulos del Sistema

- **Agenda Activa y Disponibilidad:** Panel visual de horarios libres y ocupados para evitar cruces entre estilistas.
- **Gestión Integral de Citas:** Registro de clientes, asignación de especialistas, reprogramación de fecha/hora y cancelación.
- **Cuadre de Caja y Facturación:** Arqueo de ingresos en tiempo real, desglose por estilista, ticket promedio y filtros temporales (Ayer, Hoy, Esta Semana, Mañana, Histórico).
- **Persistencia en la Nube:** Almacenamiento continuo en Supabase PostgreSQL con modo híbrido de contingencia.
- **Interfaz Revolut:** Estética limpia con contraste Stark Black (#000000) y Canvas Light (#ffffff), tipografía Inter, bordes de 1px y componentes Bento Grid.

---

## Stack Tecnológico

- **Backend:** Java (HTTP Server nativo, JDBC PostgreSQL).
- **Base de Datos:** PostgreSQL en la nube (Supabase).
- **Frontend:** HTML5 semántico, CSS3 Vanilla y JavaScript moderno (ES6+).
- **Entorno:** Compatible con Visual Studio Code, NetBeans, IntelliJ IDEA y Eclipse.

---

## Ejecución del Proyecto

### 1. Requisitos Previos
- Java JDK 17 o superior instalado.
- Visual Studio Code o cualquier IDE Java compatible.

### 2. Ejecutar desde el IDE
1. Abrir la carpeta del proyecto en Visual Studio Code.
2. Abrir el archivo `MainWeb.java` ubicado en `src/proyectodp/main/MainWeb.java`.
3. Presionar el botón de ejecución **Run** (o presionar `F5`). Las librerías de PostgreSQL ya están configuradas en el entorno.

### 3. Ejecutar por Terminal
```bash
java -cp "build/classes;lib/postgresql-42.7.3.jar" proyectodp.main.MainWeb
```

### 4. Acceso Web
Abrir en el navegador:
```text
http://localhost:8080
```

---

## Proyecto
Proyecto final desarrollado para la carrera de Ingeniería de Sistemas e Informática - Universidad Tecnológica del Perú (UTP).
