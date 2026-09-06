---
name: premium-ui-ux-design
description: >-
  Skill maestra para diseño y maquetación de interfaces web modernas, lujosas y
  altamente interactivas (Glassmorphism, Dark Themes, Micro-animaciones y Tipografía de Vanguardia).
---

# Skill: Premium UI/UX Design System

Utiliza esta skill cuando el usuario solicite crear, mejorar o rediseñar vistas, componentes o interfaces de usuario frontend.

## Filosofía de Diseño
El objetivo principal es entregar interfaces que causen una impresión inmediata de calidad superior ("efecto WOW"). Cada detalle visual debe comunicar profesionalismo y elegancia.

---

## 1. Tokens de Color y Variables CSS
Declara siempre variables CSS en `:root` con valores cuidadosamente calculados:

```css
:root {
  /* Fondos y Superficies */
  --bg-main: #0c0e12;
  --bg-surface: #141720;
  --bg-card: rgba(22, 26, 36, 0.85);
  --bg-card-hover: rgba(30, 36, 50, 0.95);

  /* Acentos de Lujo */
  --accent-gold: #d4af37;
  --accent-gold-light: #f3e5ab;
  --accent-gold-glow: rgba(212, 175, 55, 0.25);
  --accent-border: rgba(212, 175, 55, 0.2);

  /* Estados y Semáforos Visuales */
  --status-free: #10b981;
  --status-busy: #ef4444;
  --status-pending: #f59e0b;
  --status-info: #3b82f6;

  /* Texto */
  --text-primary: #f8fafc;
  --text-secondary: #94a3b8;
  --text-muted: #64748b;
}
```

---

## 2. Técnicas de Profundidad Visual (Glassmorphism & Glow)
- **Efecto Esmerilado**:
  ```css
  .card {
    background: var(--bg-card);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid var(--accent-border);
    border-radius: 16px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
  }
  ```
- **Luces Ambientales Difusas**:
  ```css
  .bg-glow {
    position: fixed;
    border-radius: 50%;
    filter: blur(140px);
    pointer-events: none;
    opacity: 0.15;
  }
  ```

---

## 3. Píldoras de Estado e Indicadores Dinámicos
- En calendarios y reservas, no usar textos planos. Usar píldoras estilizadas con puntos luminosos (`glow dot`):
  ```css
  .slot-pill {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.45rem 0.85rem;
    border-radius: 999px;
    font-weight: 600;
    transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .slot-free {
    background: rgba(16, 185, 129, 0.12);
    border: 1px solid rgba(16, 185, 129, 0.3);
    color: #34d399;
  }
  .slot-busy {
    background: rgba(239, 68, 68, 0.12);
    border: 1px solid rgba(239, 68, 68, 0.3);
    color: #f87171;
  }
  ```

---

## 4. Modales y Toasts
- **Modales**: Con fondo desenfocado oscuro (`backdrop-filter: blur(8px)`) y animación de entrada suave (`scale(0.95) -> scale(1)`).
- **Notificaciones Toast**: Mensajes no intrusivos que entran desde un costado, con íconos claros y temporizador de auto-cierre con fade out.
