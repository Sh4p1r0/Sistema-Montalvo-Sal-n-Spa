-- ==========================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS - MONTALVO SALÓN & SPA
-- Plataforma: PostgreSQL (Supabase)
-- ==========================================================

-- 1. Tabla de Usuarios y Personal del Salón
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    rol VARCHAR(30) NOT NULL DEFAULT 'RECEPCIONISTA',
    nombre VARCHAR(100) NOT NULL,
    area_servicio VARCHAR(50) NOT NULL
);

-- Asegurar que la restricción antigua permita RECEPCIONISTA
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;

-- 2. Tabla de Servicios Ofrecidos
CREATE TABLE IF NOT EXISTS servicios (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL,
    duracion_minutos INT NOT NULL,
    area VARCHAR(50) NOT NULL
);

-- 3. Tabla de Citas y Agenda
CREATE TABLE IF NOT EXISTS citas (
    id VARCHAR(50) PRIMARY KEY,
    cliente VARCHAR(100) NOT NULL,
    fecha DATE NOT NULL,
    hora VARCHAR(10) NOT NULL,
    estilista VARCHAR(100) NOT NULL,
    servicio VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente' CHECK (estado IN ('Pendiente', 'Reprogramada', 'Atendida', 'Cancelada', 'Finalizada')),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para búsqueda rápida por fecha y estilista
CREATE INDEX IF NOT EXISTS idx_citas_fecha ON citas(fecha);
CREATE INDEX IF NOT EXISTS idx_citas_estilista ON citas(estilista);
CREATE INDEX IF NOT EXISTS idx_citas_estado ON citas(estado);

-- ==========================================================
-- DATOS INICIALES (SEMILLA)
-- ==========================================================

-- Insertar Usuario Recepcionista Principal y Especialistas del Salón
INSERT INTO usuarios (username, password, rol, nombre, area_servicio) VALUES
    ('recep', 'montalvo', 'RECEPCIONISTA', 'Recepción Principal', 'Caja y Atención General'),
    ('carlos', '123456', 'ESTILISTA', 'Carlos Mendoza', 'Corte y Barbería'),
    ('sofia', '123456', 'ESTILISTA', 'Sofia Silva', 'Peinados y Planchado'),
    ('camila', '123456', 'ESTILISTA', 'Camila Rios', 'Manicura y Uñas'),
    ('diego', '123456', 'ESTILISTA', 'Diego Morales', 'Spa y Masajes')
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password, rol = EXCLUDED.rol, nombre = EXCLUDED.nombre;

-- Insertar Catálogo de Servicios
INSERT INTO servicios (codigo, nombre, precio, duracion_minutos, area) VALUES
    ('CORTE', 'Corte y Estilizado Exclusivo', 35.00, 45, 'Corte y Barbería'),
    ('PLANCHADO', 'Planchado & Keratina Express', 50.00, 60, 'Peinados y Planchado'),
    ('MANICURA', 'Manicura Spa Rusa & Esmaltado', 28.00, 45, 'Manicura y Uñas'),
    ('MASAJE', 'Masaje Terapéutico & Descontracturante', 65.00, 50, 'Spa y Masajes')
ON CONFLICT (codigo) DO NOTHING;

-- Insertar Citas Demo para hoy y días próximos
INSERT INTO citas (id, cliente, fecha, hora, estilista, servicio, precio, estado) VALUES
    ('CIT-1001', 'Luciana Ramos', CURRENT_DATE, '10:00', 'Carlos Mendoza', 'Corte y Estilizado Exclusivo', 35.00, 'Pendiente'),
    ('CIT-1002', 'Andrea Paredes', CURRENT_DATE, '11:30', 'Sofia Silva', 'Planchado & Keratina Express', 50.00, 'Atendida'),
    ('CIT-1003', 'Valeria Castro', CURRENT_DATE, '15:00', 'Camila Rios', 'Manicura Spa Rusa & Esmaltado', 28.00, 'Pendiente'),
    ('CIT-1004', 'Gabriel Mendoza', CURRENT_DATE + INTERVAL '1 day', '14:00', 'Carlos Mendoza', 'Corte y Estilizado Exclusivo', 35.00, 'Pendiente'),
    ('CIT-1005', 'Paola Navarro', CURRENT_DATE + INTERVAL '1 day', '16:30', 'Diego Morales', 'Masaje Terapéutico & Descontracturante', 65.00, 'Pendiente')
ON CONFLICT (id) DO NOTHING;
