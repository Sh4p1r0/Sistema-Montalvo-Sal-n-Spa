---
name: bento-dashboard-design
description: >-
  Especialidad en diseño de paneles administrativos, métricas en tiempo real,
  KPIs ejecutivos y agendas interactivas mediante estructuras Bento Grid modernas.
---

# Skill: Bento Grid & Dashboard Design

Utiliza esta skill al diseñar pantallas de métricas, paneles de control, reportes ejecutivos o resúmenes estadísticos.

---

## 1. Arquitectura Bento Grid
Organiza la información en bloques modulares que aprovechan el espacio de forma asimétrica y ordenada:

```css
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.25rem;
}

.kpi-card {
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 1.25rem 1.5rem;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  transition: border-color 0.2s;
}

.kpi-card:hover {
  border-color: var(--accent-border);
}

.kpi-label {
  font-size: 0.78rem;
  color: var(--text-secondary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-value {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--text-primary);
}
```

---

## 2. Barras de Desglose y Progreso Animadas
- En estadísticas por categoría o especialista, usar barras de porcentaje con fondo tenue y relleno degradado:
  ```css
  .breakdown-bar-bg {
    height: 8px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 999px;
    overflow: hidden;
  }
  .breakdown-bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #9e7d18, #d4af37);
    border-radius: 999px;
    transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  }
  ```

---

## 3. Principio de Actualización en Vivo (Zero-Reload)
- Los paneles de estadísticas deben actualizarse mediante peticiones asíncronas (`fetch` / polling cada pocos segundos o al registrar una acción) sin refrescar toda la página web, manteniendo la fluidez de la experiencia de usuario.
