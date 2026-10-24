/**
 * MONTALVO SALÓN & SPA - GESTIÓN CENTRALIZADA DE CITAS & RECEPCIÓN
 * Arquitectura conectada al Backend Java con Patrones de Diseño GoF:
 * - State: Transiciones de citas (Pendiente -> Atendida -> Finalizada/Archivada)
 * - Builder & Factory: Construcción robusta de Citas y Servicios
 * - Facade: Fachada unificada de operaciones del salón
 * - Singleton & DAO: Conexión persistente a PostgreSQL (Supabase) con fallback en memoria
 * - Proxy: Control de acceso y seguridad
 */

const API_BASE = window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')
  ? window.location.origin
  : 'http://localhost:8080';

// Estado global de la aplicación
let currentUser = null; // Usuario autenticado
let authToken = null;   // Token de sesión emitido por el servidor
let citas = [];
let servicios = [];
let estilistas = [];
let fechaSeleccionada = new Date().toISOString().split('T')[0]; // Hoy por defecto

// Elementos DOM - Sesión y Recepción
const modalLogin = document.getElementById('modalLogin');
const formLogin = document.getElementById('formLogin');
const loginUsername = document.getElementById('loginUsername');
const loginPassword = document.getElementById('loginPassword');
const loginError = document.getElementById('loginError');
const userAvatar = document.getElementById('userAvatar');
const userName = document.getElementById('userName');
const userRoleTag = document.getElementById('userRoleTag');
const btnLogout = document.getElementById('btnLogout');
const btnToggleStats = document.getElementById('btnToggleStats');
const panelAdminStats = document.getElementById('panelAdminStats');
const btnCerrarStats = document.getElementById('btnCerrarStats');
const groupFiltroEstilista = document.getElementById('groupFiltroEstilista');

// Elementos DOM - Calendario y Filtros
const inputFechaFiltro = document.getElementById('inputFechaFiltro');
const chipAyer = document.getElementById('chipAyer');
const chipHoy = document.getElementById('chipHoy');
const chipSemana = document.getElementById('chipSemana');
const chipManana = document.getElementById('chipManana');
const chipTodas = document.getElementById('chipTodas');
const slotsVisualizer = document.getElementById('slotsVisualizer');
const citasGrid = document.getElementById('citasGrid');
const emptyState = document.getElementById('emptyState');
const contadorCitas = document.getElementById('contadorCitas');
const citasTitulo = document.getElementById('citasTitulo');

const statHoy = document.getElementById('statHoy');
const statPendientes = document.getElementById('statPendientes');
const statAtendidas = document.getElementById('statAtendidas');

// Elementos DOM - Cuadre y Auditoría de Caja (Recepción)
const cardCajaHeader = document.getElementById('cardCajaHeader');
const statCajaLabel = document.getElementById('statCajaLabel');
const statCajaValor = document.getElementById('statCajaValor');
const btnAbrirCaja = document.getElementById('btnAbrirCaja');
const modalCierreCaja = document.getElementById('modalCierreCaja');
const btnCerrarModalCaja = document.getElementById('btnCerrarModalCaja');
const btnCerrarCajaFooter = document.getElementById('btnCerrarCajaFooter');
const btnExportarCaja = document.getElementById('btnExportarCaja');

let periodoSeleccionado = 'hoy'; // 'ayer', 'hoy', 'semana', 'manana', 'todas', 'fecha'
let ultimaCajaData = null; // Último arqueo cargado (para imprimir / exportar)

const inputBuscar = document.getElementById('inputBuscar');
const filtroEstado = document.getElementById('filtroEstado');
const filtroEstilista = document.getElementById('filtroEstilista');

// Modales
const modalNuevaCita = document.getElementById('modalNuevaCita');
const modalReprogramar = document.getElementById('modalReprogramar');

const formNuevaCita = document.getElementById('formNuevaCita');
const formReprogramar = document.getElementById('formReprogramar');

const selectServicio = document.getElementById('selectServicio');
const selectEstilista = document.getElementById('selectEstilista');
const previewPrecio = document.getElementById('previewPrecio');
const previewDuracion = document.getElementById('previewDuracion');

// Variable de auto-actualización en vivo para estadísticas
let statsPollInterval = null;

// Cliente HTTP con token de sesión: agrega Authorization y maneja el 401
async function apiFetch(url, options = {}) {
  const opts = { ...options, headers: { ...(options.headers || {}) } };
  if (authToken) {
    opts.headers['Authorization'] = `Bearer ${authToken}`;
  }
  const res = await fetch(url, opts);

  if (res.status === 401) {
    limpiarSesionLocal();
    citas = [];
    renderizarCitas();
    resetearContadoresHeader();
    abrirModalLogin();
    mostrarErrorLogin('Tu sesión expiró. Inicia sesión nuevamente.');
    throw new Error('Sesión expirada');
  }
  return res;
}

// Inicialización de la aplicación
document.addEventListener('DOMContentLoaded', async () => {
  inputFechaFiltro.value = fechaSeleccionada;
  document.getElementById('inputFechaCita').value = fechaSeleccionada;
  document.getElementById('reprogramNuevaFecha').value = fechaSeleccionada;

  await cargarServicios();

  configurarTema();
  configurarAutenticacion();
  configurarEventos();
});

// ==========================================================
// 0. TEMA CLARO / OSCURO (PERSISTENTE)
// ==========================================================
function aplicarTema(tema) {
  const oscuro = tema === 'dark';
  document.documentElement.setAttribute('data-theme', tema);
  try {
    localStorage.setItem('montalvo-theme', tema);
  } catch (e) {
    /* localStorage no disponible: el tema solo dura la sesión */
  }

  const btn = document.getElementById('btnThemeToggle');
  if (btn) {
    const texto = oscuro ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro';
    btn.setAttribute('aria-label', texto);
    btn.setAttribute('title', texto);
  }
}

function configurarTema() {
  const actual = document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
  aplicarTema(actual);

  const btn = document.getElementById('btnThemeToggle');
  if (!btn) return;

  btn.addEventListener('click', () => {
    const esOscuro = document.documentElement.getAttribute('data-theme') === 'dark';
    aplicarTema(esOscuro ? 'light' : 'dark');
  });
}

// ==========================================================
// 1. SISTEMA DE AUTENTICACIÓN Y ACCESO (PATRÓN PROXY)
// ==========================================================
function configurarAutenticacion() {
  // Restaurar sesión guardada validándola contra el servidor
  const tokenGuardado = localStorage.getItem('montalvo_token');
  if (tokenGuardado) {
    authToken = tokenGuardado;
    iniciarSesionGuardada();
  } else {
    abrirModalLogin();
  }

  // Formulario de login
  formLogin.addEventListener('submit', async (e) => {
    e.preventDefault();
    loginError.classList.add('hidden');
    const u = loginUsername.value.trim();
    const p = loginPassword.value.trim();

    try {
      const res = await fetch(`${API_BASE}/api/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: u, password: p })
      });
      const data = await res.json();
      if (data.success && data.usuario && data.token) {
        await establecerSesion(data.usuario, data.token);
      } else {
        mostrarErrorLogin(data.error || 'Credenciales inválidas.');
      }
    } catch (err) {
      mostrarErrorLogin('No se pudo conectar con el servidor. Verifica que el backend esté activo.');
    }
  });

  // Botón de acceso rápido (1 Clic)
  document.querySelectorAll('.btn-quick-user').forEach(btn => {
    btn.addEventListener('click', () => {
      loginUsername.value = btn.dataset.user || 'recep';
      loginPassword.value = btn.dataset.pass || 'montalvo';
      formLogin.dispatchEvent(new Event('submit'));
    });
  });

  // Botón de cerrar sesión
  btnLogout.addEventListener('click', () => {
    cerrarSesion();
  });
}

// Valida el token guardado y, si sigue vigente, restaura la sesión
async function iniciarSesionGuardada() {
  try {
    const res = await fetch(`${API_BASE}/api/sesion`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    if (!res.ok) throw new Error('Sesión expirada');

    const data = await res.json();
    currentUser = data.usuario;
    localStorage.setItem('montalvo_user', JSON.stringify(currentUser));
    cerrarModalLogin();
    aplicarPerfilUsuario();
    await cargarEstilistas();
    await cargarCitas();
  } catch (err) {
    limpiarSesionLocal();
    abrirModalLogin();
  }
}

function abrirModalLogin() {
  modalLogin.classList.remove('hidden');
  loginError.classList.add('hidden');
}

function cerrarModalLogin() {
  modalLogin.classList.add('hidden');
}

function mostrarErrorLogin(msg) {
  loginError.textContent = msg;
  loginError.classList.remove('hidden');
}

async function establecerSesion(user, token) {
  authToken = token || null;
  currentUser = user;
  try {
    localStorage.setItem('montalvo_user', JSON.stringify(user));
    if (token) localStorage.setItem('montalvo_token', token);
  } catch (e) {
    /* localStorage no disponible */
  }
  cerrarModalLogin();
  aplicarPerfilUsuario();
  await cargarEstilistas();
  await cargarCitas();
  mostrarToast(`¡Sesión activa: ${user.nombre}!`, 'success');
}

function limpiarSesionLocal() {
  authToken = null;
  currentUser = null;
  try {
    localStorage.removeItem('montalvo_token');
    localStorage.removeItem('montalvo_user');
  } catch (e) {
    /* localStorage no disponible */
  }
}

async function cerrarSesion() {
  const token = authToken;
  limpiarSesionLocal();

  if (token) {
    fetch(`${API_BASE}/api/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    }).catch(() => {});
  }

  citas = [];
  renderizarCitas();
  actualizarContadoresHeader();
  abrirModalLogin();
  mostrarToast('Sesión cerrada.', 'info');
}

function aplicarPerfilUsuario() {
  if (!currentUser) return;

  userName.textContent = currentUser.nombre;
  userAvatar.textContent = currentUser.nombre.charAt(0).toUpperCase();
  userRoleTag.textContent = currentUser.rol || 'RECEPCIONISTA';

  const esEstilista = (currentUser.rol || '').toUpperCase() === 'ESTILISTA';

  btnToggleStats.classList.toggle('hidden', esEstilista);
  groupFiltroEstilista.classList.toggle('hidden', esEstilista);

  const btnNuevaCita = document.getElementById('btnNuevaCita');
  if (btnNuevaCita) btnNuevaCita.classList.toggle('hidden', esEstilista);
  if (cardCajaHeader) cardCajaHeader.classList.toggle('hidden', esEstilista);
}

// ==========================================================
// 2. CARGA DE CATÁLOGOS (FACTORY & PROXY)
// ==========================================================
async function cargarServicios() {
  try {
    const res = await apiFetch(`${API_BASE}/api/servicios`);
    servicios = await res.json();
    
    selectServicio.innerHTML = servicios.map(s => `
      <option value="${s.codigo}" data-precio="${s.precio}" data-duracion="${s.duracion}">
        ${s.nombre} (S/. ${s.precio.toFixed(2)})
      </option>
    `).join('');

    actualizarPreviewServicio();
  } catch (err) {
    console.warn('Usando catálogo local de servicios (modo offline)', err);
    servicios = [
      { codigo: 'CORTE', nombre: 'Corte y Estilizado Exclusivo', precio: 35.00, duracion: 45 },
      { codigo: 'PLANCHADO', nombre: 'Planchado & Keratina Express', precio: 50.00, duracion: 60 },
      { codigo: 'MANICURA', nombre: 'Manicura Spa Rusa & Esmaltado', precio: 28.00, duracion: 45 },
      { codigo: 'MASAJE', nombre: 'Masaje Terapéutico & Descontracturante', precio: 65.00, duracion: 50 }
    ];
    selectServicio.innerHTML = servicios.map(s => `
      <option value="${s.codigo}" data-precio="${s.precio}" data-duracion="${s.duracion}">
        ${s.nombre} (S/. ${s.precio.toFixed(2)})
      </option>
    `).join('');
    actualizarPreviewServicio();
  }
}

async function cargarEstilistas() {
  try {
    const res = await apiFetch(`${API_BASE}/api/estilistas`);
    estilistas = await res.json();
  } catch (err) {
    estilistas = [
      { id: 1, nombre: 'Carlos Mendoza', especialidad: 'Corte y Barbería', area: 'Corte y Barbería' },
      { id: 2, nombre: 'Sofia Silva', especialidad: 'Peinados y Planchado', area: 'Peinados y Planchado' },
      { id: 3, nombre: 'Camila Rios', especialidad: 'Manicura y Uñas', area: 'Manicura y Uñas' },
      { id: 4, nombre: 'Diego Morales', especialidad: 'Spa y Masajes', area: 'Spa y Masajes' }
    ];
  }

  // Poblar selectores de especialistas
  filtroEstilista.innerHTML = '<option value="TODOS">Todos los Especialistas</option>' + 
    estilistas.map(e => `<option value="${e.nombre}">${e.nombre} (${e.area || e.especialidad})</option>`).join('');

  selectEstilista.innerHTML = estilistas.map(e => `
    <option value="${e.nombre}">${e.nombre} - ${e.area || e.especialidad}</option>
  `).join('');
}

// ==========================================================
// 3. CARGA Y SINCRONIZACIÓN DE CITAS ACTIVAS
// ==========================================================
async function cargarCitas() {
  if (!currentUser) return;

  try {
    let url = `${API_BASE}/api/citas?`;
    if (periodoSeleccionado === 'ayer') {
      url += 'periodo=ayer&';
    } else if (periodoSeleccionado === 'semana') {
      url += 'periodo=semana&';
    } else if (periodoSeleccionado === 'todas') {
      url += 'todas=true&';
    } else if (fechaSeleccionada) {
      url += `fecha=${fechaSeleccionada}&`;
    }

    const res = await apiFetch(url);
    if (!res.ok) throw new Error('Error al conectar con la base de datos de citas.');
    citas = await res.json();

    renderizarCitas();
    actualizarContadoresHeader();
    renderizarSlotsVisualizer();

    if (!panelAdminStats.classList.contains('hidden')) {
      cargarEstadisticasAdmin();
    }
  } catch (err) {
    console.error('Error al cargar citas:', err);
    mostrarToast('Sincronizando en modo offline resiliente.', 'info');
  }
}

// ==========================================================
// 4. RENDERIZADO DE CITAS EN EL GRID
// ==========================================================
function renderizarCitas() {
  const termino = inputBuscar.value.toLowerCase().trim();
  const estadoFiltro = filtroEstado.value;
  const estilistaFiltro = filtroEstilista.value;

  const citasFiltradas = citas.filter(c => {
    // Citas que ya estén en estado Finalizada quedan archivadas de la vista diaria
    if (c.estado === 'Finalizada') return false;

    const coincideBusqueda = c.nombreCliente.toLowerCase().includes(termino) ||
                             c.tipoServicio.toLowerCase().includes(termino) ||
                             c.estilista.toLowerCase().includes(termino);

    const coincideEstado = (estadoFiltro === 'TODOS') || (c.estado === estadoFiltro);
    const coincideEstilista = (estilistaFiltro === 'TODOS') || (c.estilista === estilistaFiltro);

    return coincideBusqueda && coincideEstado && coincideEstilista;
  });

  contadorCitas.textContent = `${citasFiltradas.length} ${citasFiltradas.length === 1 ? 'cita activa' : 'citas activas'}`;

  if (citasFiltradas.length === 0) {
    citasGrid.innerHTML = '';
    emptyState.classList.remove('hidden');
    return;
  }

  emptyState.classList.add('hidden');

  citasGrid.innerHTML = citasFiltradas.map(c => {
    // Determinar clase de estado y píldora con punto brillante pulsante
    let estadoClass = 'pending';
    if (c.estado === 'Atendida') estadoClass = 'active';
    else if (c.estado === 'Reprogramada') estadoClass = 'reprogrammed';
    else if (c.estado === 'Cancelada') estadoClass = 'canceled';

    const esAtendida = c.estado === 'Atendida';
    const esPendienteOReprog = c.estado === 'Pendiente' || c.estado === 'Reprogramada';

    // Botón especial de FINALIZAR (Scale(1.02) + Glow con gradiente)
    const botonFinalizar = esAtendida ? `
      <button class="btn-finalize-luxe" onclick="finalizarCita('${c.id}')" title="Cobrar y archivar cita con transición suave">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
        Finalizar Cita
      </button>
    ` : '';

    return `
      <div class="bento-appointment-card card" id="card-${c.id}" data-id="${c.id}">
        <div class="card-top-row">
          <div class="time-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            <span>${c.hora}</span>
          </div>
          <div class="status-pill ${estadoClass}">
            <span class="status-dot"></span>
            <span>${c.estado === 'Atendida' ? 'En Atención' : c.estado}</span>
          </div>
        </div>

        <div class="card-content">
          <h3 class="client-name">${escapeHtml(c.nombreCliente)}</h3>
          <p class="service-title">${escapeHtml(c.tipoServicio)}</p>
          
          <div class="card-meta-row">
            <span class="meta-item">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              ${escapeHtml(c.estilista)}
            </span>
            <span class="meta-item">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line></svg>
              ${c.fecha}
            </span>
          </div>
        </div>

        <div class="card-bottom-row">
          <div class="price-container">
            <span class="price-label">Monto Total</span>
            <span class="price-value">S/. ${c.precio.toFixed(2)}</span>
          </div>

          <div class="card-actions-group">
            ${botonFinalizar}

            ${esPendienteOReprog ? `
              <button class="btn-minimal" onclick="abrirModalReprogramar('${c.id}')" title="Reprogramar fecha u hora">
                Reprogramar
              </button>
              <button class="btn-minimal btn-atender" onclick="cambiarEstadoCita('${c.id}', 'atender')" title="Marcar como Atendida">
                Atender
              </button>
              <button class="btn-minimal btn-cancelar" onclick="cambiarEstadoCita('${c.id}', 'cancelar')" title="Cancelar Cita">
                &times;
              </button>
            ` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// ==========================================================
// 5. ACCIONES DE CITAS (PATRÓN STATE)
// ==========================================================

// Finalizar cita con animación fluida de 400ms (opacity 0, translateY 10px)
async function finalizarCita(id) {
  const card = document.getElementById(`card-${id}`);
  if (card) {
    card.classList.add('card-dismissing');
  }

  try {
    const res = await apiFetch(`${API_BASE}/api/citas/finalizar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id })
    });
    const data = await res.json();

    if (data.success) {
      mostrarToast('Cita finalizada y cobrada. Archivada de la agenda.', 'success');
      setTimeout(async () => {
        if (card) card.remove();
        await cargarCitas();
      }, 400);
    } else {
      if (card) card.classList.remove('card-dismissing');
      mostrarToast(data.error || 'No se pudo finalizar la cita.', 'error');
    }
  } catch (err) {
    console.error('Error al finalizar cita:', err);
    const cita = citas.find(c => c.id === id);
    if (cita) cita.estado = 'Finalizada';
    mostrarToast('Cita finalizada y archivada en memoria.', 'success');
    setTimeout(() => {
      if (card) card.remove();
      renderizarCitas();
      actualizarContadoresHeader();
      renderizarSlotsVisualizer();
    }, 400);
  }
}

async function cambiarEstadoCita(id, accion) {
  try {
    const res = await apiFetch(`${API_BASE}/api/citas/estado`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, accion })
    });
    const data = await res.json();
    if (data.success) {
      mostrarToast(data.mensaje || 'Estado actualizado con éxito.', 'success');
      await cargarCitas();
    } else {
      mostrarToast(data.error || 'No se pudo actualizar el estado.', 'error');
    }
  } catch (err) {
    console.error('Error al cambiar estado:', err);
    mostrarToast('Error al comunicar con el servidor.', 'error');
  }
}

// ==========================================================
// 6. CREACIÓN Y REPROGRAMACIÓN DE CITAS
// ==========================================================
formNuevaCita.addEventListener('submit', async (e) => {
  e.preventDefault();

  const cliente = document.getElementById('inputCliente').value.trim();
  const tipoServicio = selectServicio.value;
  const estilista = selectEstilista.value;
  const fecha = document.getElementById('inputFechaCita').value;
  const hora = document.getElementById('inputHoraCita').value;

  try {
    const res = await apiFetch(`${API_BASE}/api/citas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cliente, fecha, hora, tipoServicio, estilista })
    });

    const data = await res.json();

    if (data.success) {
      mostrarToast('¡Cita agendada con éxito!', 'success');
      cerrarModalNueva();
      formNuevaCita.reset();
      
      // Sincronizar vista con la fecha de la cita creada
      fechaSeleccionada = fecha;
      inputFechaFiltro.value = fecha;
      desmarcarChips();
      
      await cargarCitas();
    } else {
      mostrarToast(data.error || 'Error al agendar cita.', 'error');
    }
  } catch (err) {
    console.error('Error al registrar cita:', err);
    mostrarToast('Error al registrar la cita.', 'error');
  }
});

function abrirModalReprogramar(id) {
  const cita = citas.find(c => c.id === id);
  if (!cita) return;

  document.getElementById('reprogramId').value = cita.id;
  document.getElementById('reprogramNuevaFecha').value = cita.fecha;
  document.getElementById('reprogramNuevaHora').value = cita.hora;

  document.getElementById('reprogramSummary').innerHTML = `
    <div style="font-size: 0.88rem; line-height: 1.6;">
      <strong>Cliente:</strong> ${escapeHtml(cita.nombreCliente)}<br>
      <strong>Servicio:</strong> ${escapeHtml(cita.tipoServicio)}<br>
      <strong>Especialista:</strong> ${escapeHtml(cita.estilista)}<br>
      <strong>Horario Actual:</strong> ${cita.fecha} a las ${cita.hora}
    </div>
  `;

  modalReprogramar.classList.remove('hidden');
}

formReprogramar.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('reprogramId').value;
  const nuevaFecha = document.getElementById('reprogramNuevaFecha').value;
  const nuevaHora = document.getElementById('reprogramNuevaHora').value;

  try {
    const res = await apiFetch(`${API_BASE}/api/citas/reprogramar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, nuevaFecha, nuevaHora })
    });
    const data = await res.json();
    if (data.success) {
      mostrarToast('Cita reprogramada con éxito.', 'success');
      cerrarModalReprogramar();
      await cargarCitas();
    } else {
      mostrarToast(data.error || 'Conflicto de horario al reprogramar.', 'error');
    }
  } catch (err) {
    console.error('Error al reprogramar:', err);
    mostrarToast('Error al conectar con el servidor.', 'error');
  }
});

// ==========================================================
// 7. PANEL DE ESTADÍSTICAS EN TIEMPO REAL (SIN REFRESCAR PÁGINA)
// ==========================================================
async function cargarEstadisticasAdmin() {
  try {
    const res = await apiFetch(`${API_BASE}/api/stats`);
    if (!res.ok) return;
    const stats = await res.json();

    document.getElementById('kpiTotal').textContent = stats.total;
    document.getElementById('kpiFinalizadas').textContent = stats.finalizadas;
    document.getElementById('kpiPendientes').textContent = stats.pendientes;
    document.getElementById('kpiIngresos').textContent = `S/. ${stats.ingresos.toFixed(2)}`;

    // Desglose por Servicio
    const serviciosBox = document.getElementById('breakdownServicios');
    const serviciosEntries = Object.entries(stats.porServicio || {});
    const maxServ = Math.max(...serviciosEntries.map(e => e[1]), 1);

    serviciosBox.innerHTML = serviciosEntries.map(([nombre, cantidad]) => {
      const porcentaje = Math.round((cantidad / maxServ) * 100);
      return `
        <div class="breakdown-item">
          <div class="breakdown-item-header">
            <span>${escapeHtml(nombre)}</span>
            <strong>${cantidad} citas</strong>
          </div>
          <div class="breakdown-bar-bg">
            <div class="breakdown-bar-fill" style="width: ${porcentaje}%;"></div>
          </div>
        </div>
      `;
    }).join('') || '<p style="color:var(--text-muted);font-size:0.8rem;">Sin citas registradas aún.</p>';

    // Desglose por Estilista
    const estilistasBox = document.getElementById('breakdownEstilistas');
    const estilistasEntries = Object.entries(stats.porEstilista || {});
    const maxEst = Math.max(...estilistasEntries.map(e => e[1]), 1);

    estilistasBox.innerHTML = estilistasEntries.map(([nombre, cantidad]) => {
      const porcentaje = Math.round((cantidad / maxEst) * 100);
      return `
        <div class="breakdown-item">
          <div class="breakdown-item-header">
            <span>${escapeHtml(nombre)}</span>
            <strong>${cantidad} citas</strong>
          </div>
          <div class="breakdown-bar-bg">
            <div class="breakdown-bar-fill" style="width: ${porcentaje}%;"></div>
          </div>
        </div>
      `;
    }).join('') || '<p style="color:var(--text-muted);font-size:0.8rem;">Sin citas registradas aún.</p>';

  } catch (err) {
    console.error('Error al cargar estadísticas en vivo:', err);
  }
}

// ==========================================================
// 8. VISUALIZADOR DE SLOTS (VERDE = LIBRE / ROJO = OCUPADO)
// Soporta citas simultáneas a la misma hora con distintos estilistas
// ==========================================================
function renderizarSlotsVisualizer() {
  const horas = ['09:00', '10:00', '11:00', '11:30', '12:30', '14:00', '15:00', '16:00', '16:30', '17:30', '18:30', '19:30'];
  const fechaObjetivo = fechaSeleccionada || new Date().toISOString().split('T')[0];
  const estilistaFiltro = filtroEstilista.value;
  const totalEspecialistas = estilistas.length > 0 ? estilistas.length : 4;

  // Filtrar citas activas para la fecha objetivo
  const citasFecha = citas.filter(c => {
    if (c.estado === 'Cancelada' || c.estado === 'Finalizada') return false;
    return c.fecha === fechaObjetivo;
  });

  slotsVisualizer.innerHTML = horas.map(h => {
    const citasEnHora = citasFecha.filter(c => c.hora === h);

    if (estilistaFiltro !== 'TODOS') {
      // 1. Vista Filtrada por un Especialista Específico
      const citaEspecialista = citasEnHora.find(c => c.estilista.toLowerCase() === estilistaFiltro.toLowerCase());
      const ocupado = !!citaEspecialista;
      const chipClass = ocupado ? 'is-busy' : 'is-free';
      const estadoTexto = ocupado ? 'Ocupado' : 'Libre';

      const tooltip = ocupado
        ? `Ocupado el ${fechaObjetivo} a las ${h} por ${estilistaFiltro} (Cliente: ${citaEspecialista.nombreCliente} - ${citaEspecialista.tipoServicio})`
        : `Horario Libre para ${estilistaFiltro} el ${fechaObjetivo} a las ${h}. Haz clic para agendar.`;

      const onclickAction = ocupado
        ? `mostrarDetalleOcupado('${h}', '${fechaObjetivo}', '${escapeHtml(estilistaFiltro)}', '${escapeHtml(citaEspecialista.nombreCliente)}')`
        : `agendarEnHorarioLibre('${h}', '${fechaObjetivo}', '${escapeHtml(estilistaFiltro)}')`;

      return `
        <div class="slot-chip ${chipClass}" onclick="${onclickAction}" title="${escapeHtml(tooltip)}">
          <span class="chip-time">${h}</span>
          <span class="chip-state">${estadoTexto}</span>
        </div>
      `;
    } else {
      // 2. Vista Global de Todos los Especialistas (Citas simultáneas permitidas)
      const estilistasOcupados = [...new Set(citasEnHora.map(c => c.estilista))];
      const disponiblesCount = Math.max(0, totalEspecialistas - estilistasOcupados.length);
      const completamenteLleno = disponiblesCount === 0;

      const chipClass = completamenteLleno ? 'is-busy' : 'is-free';
      const estadoTexto = completamenteLleno
        ? 'Ocupado'
        : (estilistasOcupados.length > 0 ? `Libre (${disponiblesCount}/${totalEspecialistas})` : 'Libre');

      const detalleOcupados = citasEnHora.map(c => `${c.estilista} (${c.nombreCliente})`).join(', ');

      const tooltip = completamenteLleno
        ? `Todos los ${totalEspecialistas} especialistas están ocupados a las ${h} el ${fechaObjetivo} (${detalleOcupados}).`
        : `${disponiblesCount} de ${totalEspecialistas} especialistas disponibles a las ${h} el ${fechaObjetivo}. Clic para agendar.`;

      const onclickAction = completamenteLleno
        ? `mostrarDetalleLleno('${h}', '${fechaObjetivo}', '${escapeHtml(detalleOcupados)}')`
        : `agendarEnHorarioLibre('${h}', '${fechaObjetivo}', 'TODOS')`;

      return `
        <div class="slot-chip ${chipClass}" onclick="${onclickAction}" title="${escapeHtml(tooltip)}">
          <span class="chip-time">${h}</span>
          <span class="chip-state">${estadoTexto}</span>
        </div>
      `;
    }
  }).join('');
}

function agendarEnHorarioLibre(hora, fecha, estilistaPreseleccionado) {
  document.getElementById('inputFechaCita').value = fecha;
  document.getElementById('inputHoraCita').value = hora;

  if (estilistaPreseleccionado && estilistaPreseleccionado !== 'TODOS') {
    selectEstilista.value = estilistaPreseleccionado;
  } else {
    // Si es global, buscar automáticamente un especialista que esté libre en ese horario
    const citasEnHora = citas.filter(c => c.fecha === fecha && c.hora === hora && c.estado !== 'Cancelada' && c.estado !== 'Finalizada');
    const ocupados = new Set(citasEnHora.map(c => c.estilista.toLowerCase()));
    const libre = estilistas.find(e => !ocupados.has(e.nombre.toLowerCase()));
    if (libre) {
      selectEstilista.value = libre.nombre;
    }
  }

  modalNuevaCita.classList.remove('hidden');
  document.getElementById('inputCliente').focus();
  mostrarToast(`Horario ${hora} libre seleccionado (${fecha}). Completa la cita.`, 'info');
}

function mostrarDetalleOcupado(hora, fecha, estilista, cliente) {
  mostrarToast(`${estilista} ya tiene una cita a las ${hora} el ${fecha} (Cliente: ${cliente}).`, 'error');
}

function mostrarDetalleLleno(hora, fecha, detalle) {
  mostrarToast(`Todos los especialistas están ocupados a las ${hora} el ${fecha}.`, 'error');
}

// ==========================================================
// 9. CONTADORES Y UTILIDADES
// ==========================================================
async function actualizarContadoresHeader() {
  const hoyStr = new Date().toISOString().split('T')[0];
  const activas = citas.filter(c => c.estado !== 'Finalizada');
  const deHoy = activas.filter(c => c.fecha === hoyStr);
  const pendientes = activas.filter(c => c.estado === 'Pendiente' || c.estado === 'Reprogramada');
  const atendidas = activas.filter(c => c.estado === 'Atendida');

  statHoy.textContent = deHoy.length;
  statPendientes.textContent = pendientes.length;
  statAtendidas.textContent = atendidas.length;

  // Sin sesión no se consulta la caja (evita bucles de 401)
  if (!currentUser) {
    resetearContadoresHeader();
    return;
  }

  // Un estilista no tiene acceso a la caja: se calcula solo con su agenda
  if ((currentUser.rol || '').toUpperCase() === 'ESTILISTA') {
    const cobradas = citas.filter(c => c.estado === 'Atendida' || c.estado === 'Finalizada');
    const total = cobradas.reduce((acc, c) => acc + (c.precio || 0), 0);
    if (statCajaLabel) statCajaLabel.textContent = 'Mis servicios';
    if (statCajaValor) statCajaValor.textContent = `S/. ${total.toFixed(2)}`;
    return;
  }

  // Actualizar Caja de Recepción en Tiempo Real
  try {
    const res = await apiFetch(`${API_BASE}/api/caja?periodo=${periodoSeleccionado}&fecha=${fechaSeleccionada}`);
    if (res.ok) {
      const data = await res.json();
      if (periodoSeleccionado === 'ayer') {
        statCajaLabel.textContent = 'Caja Ayer';
      } else if (periodoSeleccionado === 'semana') {
        statCajaLabel.textContent = 'Caja Semana';
      } else if (periodoSeleccionado === 'todas') {
        statCajaLabel.textContent = 'Caja Histórica';
      } else if (fechaSeleccionada && fechaSeleccionada !== hoyStr) {
        statCajaLabel.textContent = `Caja ${fechaSeleccionada.substring(5)}`;
      } else {
        statCajaLabel.textContent = 'Caja Hoy';
      }
      statCajaValor.textContent = `S/. ${data.totalRecaudado.toFixed(2)}`;
    }
  } catch (err) {
    const cobradas = citas.filter(c => c.estado === 'Atendida' || c.estado === 'Finalizada');
    const total = cobradas.reduce((acc, c) => acc + (c.precio || 0), 0);
    statCajaValor.textContent = `S/. ${total.toFixed(2)}`;
  }
}

function resetearContadoresHeader() {
  statHoy.textContent = '0';
  statPendientes.textContent = '0';
  statAtendidas.textContent = '0';
  if (statCajaValor) statCajaValor.textContent = 'S/. 0.00';
  if (statCajaLabel) statCajaLabel.textContent = 'Caja Hoy';
}

async function abrirModalCaja() {
  try {
    const res = await apiFetch(`${API_BASE}/api/caja?periodo=${periodoSeleccionado}&fecha=${fechaSeleccionada}`);
    if (!res.ok) throw new Error('No se pudo cargar los datos de caja.');
    const data = await res.json();
    ultimaCajaData = data;

    document.getElementById('cajaPeriodoLabel').textContent = data.periodo || 'Período Actual';
    document.getElementById('cajaModalTotal').textContent = `S/. ${data.totalRecaudado.toFixed(2)}`;
    document.getElementById('cajaModalServiciosCount').textContent = data.cantidadServicios;

    const promedio = data.cantidadServicios > 0 ? (data.totalRecaudado / data.cantidadServicios) : 0;
    document.getElementById('cajaModalTicketPromedio').textContent = `S/. ${promedio.toFixed(2)}`;

    // Renderizar desglose por estilista
    const stylistGrid = document.getElementById('cajaStylistGrid');
    const estilistasEntries = Object.entries(data.facturacionEstilistas || {});
    if (estilistasEntries.length === 0) {
      stylistGrid.innerHTML = '<p style="color:var(--text-muted);font-size:0.82rem;grid-column:1/-1;">Sin cobros registrados en este período.</p>';
    } else {
      stylistGrid.innerHTML = estilistasEntries.map(([nom, monto]) => `
        <div class="caja-stylist-chip">
          <span class="caja-stylist-name">${escapeHtml(nom)}</span>
          <span class="caja-stylist-amount">S/. ${monto.toFixed(2)}</span>
        </div>
      `).join('');
    }

    // Renderizar tabla de citas cobradas
    const tbody = document.getElementById('cajaTablaCitas');
    if (!data.citas || data.citas.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:1.5rem;">No hay servicios cobrados o atendidos en este período.</td></tr>';
    } else {
      tbody.innerHTML = data.citas.map(c => `
        <tr>
          <td><strong>${c.hora}</strong></td>
          <td>${c.fecha}</td>
          <td>${escapeHtml(c.nombreCliente)}</td>
          <td>${escapeHtml(c.tipoServicio)}</td>
          <td>${escapeHtml(c.estilista)}</td>
          <td class="tabular-price">S/. ${c.precio.toFixed(2)}</td>
          <td>
            <span class="status-pill ${c.estado === 'Finalizada' ? 'canceled' : 'active'}" style="font-size:0.75rem;padding:0.2rem 0.6rem;">
              ${c.estado}
            </span>
          </td>
        </tr>
      `).join('');
    }

    modalCierreCaja.classList.remove('hidden');
  } catch (err) {
    console.error('Error al abrir modal caja:', err);
    mostrarToast('No se pudo conectar para obtener el arqueo de caja.', 'error');
  }
}

function cerrarModalCaja() {
  modalCierreCaja.classList.add('hidden');
}

// ==========================================================
// 7.1 IMPRESIÓN DEL ARQUEO DE CAJA (A4)
// ==========================================================
function etiquetaPeriodoActual() {
  if (periodoSeleccionado === 'ayer') return 'Ayer';
  if (periodoSeleccionado === 'semana') return 'Esta semana';
  if (periodoSeleccionado === 'manana') return 'Mañana';
  if (periodoSeleccionado === 'todas') return 'Histórico completo';
  if (periodoSeleccionado === 'fecha' && fechaSeleccionada) return `Fecha ${fechaSeleccionada}`;
  return 'Hoy';
}

function construirReporteImpresion(data) {
  const citas = Array.isArray(data.citas) ? data.citas : [];
  const total = Number(data.totalRecaudado || 0);
  const cantidad = Number(data.cantidadServicios || citas.length || 0);
  const promedio = cantidad > 0 ? total / cantidad : 0;
  const periodo = data.periodo || etiquetaPeriodoActual();
  const generado = new Date().toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' });

  const filas = citas.map(c => `
    <tr>
      <td class="pr-center">${escapeHtml(c.hora || '')}</td>
      <td class="pr-center">${escapeHtml(c.fecha || '')}</td>
      <td>${escapeHtml(c.nombreCliente || '')}</td>
      <td>${escapeHtml(c.tipoServicio || '')}</td>
      <td>${escapeHtml(c.estilista || '')}</td>
      <td class="pr-num">S/. ${Number(c.precio || 0).toFixed(2)}</td>
      <td class="pr-center">${escapeHtml(c.estado || '')}</td>
    </tr>
  `).join('');

  return `
    <div class="pr-header">
      <div class="pr-brand">
        <div class="pr-logo">M</div>
        <div>
          <strong>MONTALVO</strong>
          <span>Salón &amp; Spa · Gestión de citas</span>
        </div>
      </div>
      <div class="pr-period">
        <strong>${escapeHtml(periodo)}</strong>
        <span>Reporte de caja</span>
      </div>
    </div>

    <h1 class="pr-title">Cuadre de Caja &amp; Arqueo de Ingresos</h1>
    <p class="pr-sub">Generado el ${escapeHtml(generado)} · Recepción Principal</p>

    <div class="pr-kpis">
      <div class="pr-kpi"><span>Total recaudado</span><strong>S/. ${total.toFixed(2)}</strong></div>
      <div class="pr-kpi"><span>Servicios cobrados</span><strong>${cantidad}</strong></div>
      <div class="pr-kpi"><span>Ticket promedio</span><strong>S/. ${promedio.toFixed(2)}</strong></div>
    </div>

    <table class="pr-table">
      <thead>
        <tr>
          <th>Hora</th>
          <th>Fecha</th>
          <th>Cliente</th>
          <th>Servicio</th>
          <th>Especialista</th>
          <th class="pr-num">Monto</th>
          <th class="pr-center">Estado</th>
        </tr>
      </thead>
      <tbody>
        ${filas || '<tr><td colspan="7" class="pr-empty">No hay servicios cobrados en este período.</td></tr>'}
      </tbody>
      <tfoot>
        <tr class="pr-total-row">
          <td colspan="5">TOTAL DEL PERÍODO</td>
          <td class="pr-num">S/. ${total.toFixed(2)}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>

    <div class="pr-footer">
      <span>Montalvo Salón &amp; Spa · Documento generado automáticamente</span>
      <span>${escapeHtml(periodo)} · ${citas.length} registro(s)</span>
    </div>
  `;
}

function imprimirReporteCaja() {
  if (!ultimaCajaData) {
    mostrarToast('Abre primero el cuadre de caja para imprimir.', 'info');
    return;
  }

  const area = document.getElementById('printArea');
  area.innerHTML = construirReporteImpresion(ultimaCajaData);
  window.print();
}

// ==========================================================
// 7.2 EXPORTACIÓN REAL A EXCEL (.XLSX, SIN LIBRERÍAS)
// ==========================================================
function xmlEscape(valor) {
  return String(valor == null ? '' : valor)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

const CRC32_TABLE = (() => {
  const tabla = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
    }
    tabla[n] = c >>> 0;
  }
  return tabla;
})();

function crc32(bytes) {
  let c = 0xFFFFFFFF;
  for (let i = 0; i < bytes.length; i++) {
    c = CRC32_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
  }
  return (c ^ 0xFFFFFFFF) >>> 0;
}

/* Genera un ZIP sin compresión con la estructura mínima de un .xlsx */
function crearZipXlsx(archivos) {
  const encoder = new TextEncoder();
  const ahora = new Date();
  const dosTime = ((ahora.getHours() & 31) << 11) | ((ahora.getMinutes() & 63) << 5) | ((ahora.getSeconds() >> 1) & 31);
  const dosDate = (((ahora.getFullYear() - 1980) & 127) << 9) | (((ahora.getMonth() + 1) & 15) << 5) | (ahora.getDate() & 31);

  const partes = [];
  const centrales = [];
  let offset = 0;

  archivos.forEach(({ nombre, contenido }) => {
    const nombreBytes = encoder.encode(nombre);
    const datos = typeof contenido === 'string' ? encoder.encode(contenido) : contenido;
    const crc = crc32(datos);

    const local = new Uint8Array(30 + nombreBytes.length);
    const lv = new DataView(local.buffer);
    lv.setUint32(0, 0x04034b50, true);
    lv.setUint16(4, 20, true);
    lv.setUint16(6, 0x0800, true);
    lv.setUint16(8, 0, true);
    lv.setUint16(10, dosTime, true);
    lv.setUint16(12, dosDate, true);
    lv.setUint32(14, crc, true);
    lv.setUint32(18, datos.length, true);
    lv.setUint32(22, datos.length, true);
    lv.setUint16(26, nombreBytes.length, true);
    lv.setUint16(28, 0, true);
    local.set(nombreBytes, 30);

    partes.push(local, datos);
    centrales.push({ nombreBytes, crc, tamano: datos.length, offset });
    offset += local.length + datos.length;
  });

  const centralBytes = [];
  let centralSize = 0;

  centrales.forEach(({ nombreBytes, crc, tamano, offset: off }) => {
    const c = new Uint8Array(46 + nombreBytes.length);
    const cv = new DataView(c.buffer);
    cv.setUint32(0, 0x02014b50, true);
    cv.setUint16(4, 20, true);
    cv.setUint16(6, 20, true);
    cv.setUint16(8, 0x0800, true);
    cv.setUint16(10, 0, true);
    cv.setUint16(12, dosTime, true);
    cv.setUint16(14, dosDate, true);
    cv.setUint32(16, crc, true);
    cv.setUint32(20, tamano, true);
    cv.setUint32(24, tamano, true);
    cv.setUint16(28, nombreBytes.length, true);
    cv.setUint16(30, 0, true);
    cv.setUint16(32, 0, true);
    cv.setUint16(34, 0, true);
    cv.setUint16(36, 0, true);
    cv.setUint32(38, 0, true);
    cv.setUint32(42, off, true);
    c.set(nombreBytes, 46);

    centralBytes.push(c);
    centralSize += c.length;
  });

  const fin = new Uint8Array(22);
  const fv = new DataView(fin.buffer);
  fv.setUint32(0, 0x06054b50, true);
  fv.setUint16(4, 0, true);
  fv.setUint16(6, 0, true);
  fv.setUint16(8, centrales.length, true);
  fv.setUint16(10, centrales.length, true);
  fv.setUint32(12, centralSize, true);
  fv.setUint32(16, offset, true);
  fv.setUint16(20, 0, true);

  return new Blob([...partes, ...centralBytes, fin], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
}

function construirXlsxCaja(data) {
  const citas = (Array.isArray(data.citas) ? data.citas : []).slice().sort((a, b) =>
    String(a.fecha || '').localeCompare(String(b.fecha || '')) ||
    String(a.hora || '').localeCompare(String(b.hora || ''))
  );

  const total = Number(data.totalRecaudado || 0);
  const cantidad = Number(data.cantidadServicios || citas.length || 0);
  const promedio = cantidad > 0 ? total / cantidad : 0;
  const periodo = data.periodo || etiquetaPeriodoActual();
  const generado = new Date().toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' });

  const COLS = ['A', 'B', 'C', 'D', 'E', 'F', 'G'];
  const ref = (i, fila) => `${COLS[i]}${fila}`;
  const cText = (r, s, v) => `<c r="${r}" s="${s}" t="inlineStr"><is><t xml:space="preserve">${xmlEscape(v)}</t></is></c>`;
  const cNum = (r, s, v, formula) => `<c r="${r}" s="${s}">${formula ? `<f>${xmlEscape(formula)}</f>` : ''}<v>${Number.isFinite(v) ? v : 0}</v></c>`;
  const cVacio = (r, s) => `<c r="${r}" s="${s}"/>`;

  const filasXml = [];
  const fila = (n, celdas, alto) => {
    filasXml.push(`<row r="${n}"${alto ? ` ht="${alto}" customHeight="1"` : ''}>${celdas.join('')}</row>`);
  };

  fila(1, [cText('A1', 1, 'MONTALVO SALÓN & SPA')], 24);
  fila(2, [cText('A2', 2, `Reporte de Caja — ${periodo}`)], 18);
  fila(3, [cText('A3', 3, `Generado el ${generado} · Recepción Principal`)], 15);
  fila(4, [], 6);

  const encabezados = ['Hora', 'Fecha', 'Cliente', 'Servicio', 'Especialista', 'Monto (S/.)', 'Estado'];
  fila(5, encabezados.map((h, i) => cText(ref(i, 5), 4, h)), 24);

  let n = 6;
  citas.forEach(c => {
    fila(n, [
      cText(ref(0, n), 6, c.hora || ''),
      cText(ref(1, n), 6, c.fecha || ''),
      cText(ref(2, n), 5, c.nombreCliente || ''),
      cText(ref(3, n), 5, c.tipoServicio || ''),
      cText(ref(4, n), 5, c.estilista || ''),
      cNum(ref(5, n), 7, Number(c.precio || 0)),
      cText(ref(6, n), 6, c.estado || '')
    ]);
    n++;
  });

  const ultimaData = n - 1;
  const filaTotal = n;
  const rangoMonto = ultimaData >= 6 ? `SUM(F6:F${ultimaData})` : '';

  fila(filaTotal, [
    cText(ref(0, filaTotal), 8, 'TOTAL DEL PERÍODO'),
    cVacio(ref(1, filaTotal), 8),
    cVacio(ref(2, filaTotal), 8),
    cVacio(ref(3, filaTotal), 8),
    cVacio(ref(4, filaTotal), 8),
    cNum(ref(5, filaTotal), 9, total, rangoMonto),
    cVacio(ref(6, filaTotal), 8)
  ], 22);

  const filaResumen = filaTotal + 2;
  fila(filaResumen, [cText(ref(0, filaResumen), 10, 'RESUMEN DEL PERÍODO')], 20);

  const resumen = [
    ['Total recaudado', total, 12],
    ['Servicios cobrados', cantidad, 14],
    ['Ticket promedio', promedio, 12],
    ['Período', periodo, 13]
  ];

  const merges = ['A1:G1', 'A2:G2', 'A3:G3', `A${filaTotal}:E${filaTotal}`];

  resumen.forEach(([etiqueta, valor, estilo], idx) => {
    const nr = filaResumen + 1 + idx;
    const celdaValor = typeof valor === 'number'
      ? cNum(ref(2, nr), estilo, valor)
      : cText(ref(2, nr), estilo, valor);

    fila(nr, [
      cText(ref(0, nr), 11, etiqueta),
      cVacio(ref(1, nr), 11),
      celdaValor
    ], 18);

    merges.push(`A${nr}:B${nr}`);
  });

  const ultimaFila = filaResumen + resumen.length;
  const mergeXml = merges.map(m => `<mergeCell ref="${m}"/>`).join('');

  const sheetXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>
<dimension ref="A1:G${ultimaFila}"/>
<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="5" topLeftCell="A6" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft" activeCell="A6" sqref="A6"/></sheetView></sheetViews>
<sheetFormatPr defaultRowHeight="15"/>
<cols>
<col min="1" max="1" width="8.5" customWidth="1"/>
<col min="2" max="2" width="12" customWidth="1"/>
<col min="3" max="3" width="26" customWidth="1"/>
<col min="4" max="4" width="34" customWidth="1"/>
<col min="5" max="5" width="22" customWidth="1"/>
<col min="6" max="6" width="14" customWidth="1"/>
<col min="7" max="7" width="13" customWidth="1"/>
</cols>
<sheetData>${filasXml.join('')}</sheetData>
<autoFilter ref="A5:G${ultimaData}"/>
<mergeCells count="${merges.length}">${mergeXml}</mergeCells>
<pageMargins left="0.5" right="0.5" top="0.6" bottom="0.6" header="0.3" footer="0.3"/>
<pageSetup paperSize="9" orientation="portrait" fitToWidth="1" fitToHeight="0"/>
</worksheet>`;

  const stylesXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1"><numFmt numFmtId="164" formatCode="#,##0.00"/></numFmts>
<fonts count="6">
<font><sz val="10.5"/><name val="Calibri"/><family val="2"/><color rgb="FF1F2937"/></font>
<font><b/><sz val="16"/><name val="Calibri"/><family val="2"/><color rgb="FF0B0D0F"/></font>
<font><b/><sz val="12"/><name val="Calibri"/><family val="2"/><color rgb="FF0B0D0F"/></font>
<font><sz val="9.5"/><name val="Calibri"/><family val="2"/><color rgb="FF6B7280"/></font>
<font><b/><sz val="10"/><name val="Calibri"/><family val="2"/><color rgb="FFFFFFFF"/></font>
<font><b/><sz val="11"/><name val="Calibri"/><family val="2"/><color rgb="FF0B0D0F"/></font>
</fonts>
<fills count="4">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF191C1F"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF3F4F6"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="2">
<border><left/><right/><top/><bottom/><diagonal/></border>
<border><left style="thin"><color rgb="FFD1D5DB"/></left><right style="thin"><color rgb="FFD1D5DB"/></right><top style="thin"><color rgb="FFD1D5DB"/></top><bottom style="thin"><color rgb="FFD1D5DB"/></bottom><diagonal/></border>
</borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="15">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="4" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
<xf numFmtId="0" fontId="5" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
<xf numFmtId="164" fontId="5" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="5" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="164" fontId="5" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
<xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="5" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>`;

  const ahoraIso = new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');

  const workbookXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<workbookPr/>
<bookViews><workbookView xWindow="0" yWindow="0" windowWidth="16000" windowHeight="9000"/></bookViews>
<sheets><sheet name="Caja" sheetId="1" r:id="rId1"/></sheets>
<definedNames><definedName name="_xlnm.Print_Titles" localSheetId="0">Caja!$5:$5</definedName></definedNames>
<calcPr calcId="191029" fullCalcOnLoad="1"/>
</workbook>`;

  const workbookRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>`;

  const contentTypes = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>`;

  const rootRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>`;

  const coreXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>Reporte de Caja - Montalvo Salon &amp; Spa</dc:title>
<dc:creator>Montalvo Salon &amp; Spa</dc:creator>
<cp:lastModifiedBy>Montalvo Salon &amp; Spa</cp:lastModifiedBy>
<dcterms:created xsi:type="dcterms:W3CDTF">${ahoraIso}</dcterms:created>
<dcterms:modified xsi:type="dcterms:W3CDTF">${ahoraIso}</dcterms:modified>
</cp:coreProperties>`;

  const appXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
<Application>Montalvo Salon &amp; Spa</Application>
</Properties>`;

  return crearZipXlsx([
    { nombre: '[Content_Types].xml', contenido: contentTypes },
    { nombre: '_rels/.rels', contenido: rootRels },
    { nombre: 'docProps/core.xml', contenido: coreXml },
    { nombre: 'docProps/app.xml', contenido: appXml },
    { nombre: 'xl/workbook.xml', contenido: workbookXml },
    { nombre: 'xl/_rels/workbook.xml.rels', contenido: workbookRels },
    { nombre: 'xl/styles.xml', contenido: stylesXml },
    { nombre: 'xl/worksheets/sheet1.xml', contenido: sheetXml }
  ]);
}

function nombreArchivoCaja(periodo) {
  const limpio = String(periodo || 'reporte')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase();
  const hoy = new Date().toISOString().split('T')[0];
  return `montalvo_caja_${limpio}_${hoy}.xlsx`;
}

function descargarBlob(blob, nombre) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  setTimeout(() => URL.revokeObjectURL(url), 3000);
}

function exportarCajaExcel() {
  if (!ultimaCajaData) {
    mostrarToast('Abre primero el cuadre de caja para exportar.', 'info');
    return;
  }

  try {
    const periodo = ultimaCajaData.periodo || etiquetaPeriodoActual();
    const blob = construirXlsxCaja(ultimaCajaData);
    descargarBlob(blob, nombreArchivoCaja(periodo));
    mostrarToast('Reporte de caja exportado a Excel.', 'success');
  } catch (err) {
    console.error('Error al exportar a Excel:', err);
    mostrarToast('No se pudo generar el archivo Excel.', 'error');
  }
}


function actualizarPreviewServicio() {
  const opt = selectServicio.options[selectServicio.selectedIndex];
  if (opt) {
    const precio = parseFloat(opt.dataset.precio || 0);
    const duracion = opt.dataset.duracion || 45;
    previewPrecio.textContent = `Total: S/. ${precio.toFixed(2)}`;
    previewDuracion.textContent = `Duración estimada: ${duracion} min`;
  }
}

function configurarEventos() {
  inputBuscar.addEventListener('input', () => renderizarCitas());
  filtroEstado.addEventListener('change', () => renderizarCitas());
  filtroEstilista.addEventListener('change', () => {
    renderizarCitas();
    renderizarSlotsVisualizer();
  });

  selectServicio.addEventListener('change', () => actualizarPreviewServicio());

  // Control de fechas dinámico: actualiza citas y el indicador Verde/Rojo
  inputFechaFiltro.addEventListener('change', (e) => {
    periodoSeleccionado = 'fecha';
    fechaSeleccionada = e.target.value;
    desmarcarChips();
    cargarCitas();
  });

  if (chipAyer) {
    chipAyer.addEventListener('click', () => {
      periodoSeleccionado = 'ayer';
      const d = new Date();
      d.setDate(d.getDate() - 1);
      fechaSeleccionada = d.toISOString().split('T')[0];
      inputFechaFiltro.value = fechaSeleccionada;
      marcarChip(chipAyer);
      cargarCitas();
    });
  }

  chipHoy.addEventListener('click', () => {
    periodoSeleccionado = 'hoy';
    fechaSeleccionada = new Date().toISOString().split('T')[0];
    inputFechaFiltro.value = fechaSeleccionada;
    marcarChip(chipHoy);
    cargarCitas();
  });

  if (chipSemana) {
    chipSemana.addEventListener('click', () => {
      periodoSeleccionado = 'semana';
      fechaSeleccionada = '';
      inputFechaFiltro.value = '';
      marcarChip(chipSemana);
      cargarCitas();
    });
  }

  chipManana.addEventListener('click', () => {
    periodoSeleccionado = 'manana';
    const d = new Date();
    d.setDate(d.getDate() + 1);
    fechaSeleccionada = d.toISOString().split('T')[0];
    inputFechaFiltro.value = fechaSeleccionada;
    marcarChip(chipManana);
    cargarCitas();
  });

  chipTodas.addEventListener('click', () => {
    periodoSeleccionado = 'todas';
    fechaSeleccionada = '';
    inputFechaFiltro.value = '';
    marcarChip(chipTodas);
    cargarCitas();
  });

  // Modal de Cuadre y Arqueo de Caja (Recepción)
  if (cardCajaHeader) cardCajaHeader.addEventListener('click', abrirModalCaja);
  if (btnAbrirCaja) btnAbrirCaja.addEventListener('click', abrirModalCaja);
  if (btnCerrarModalCaja) btnCerrarModalCaja.addEventListener('click', cerrarModalCaja);
  if (btnCerrarCajaFooter) btnCerrarCajaFooter.addEventListener('click', cerrarModalCaja);
  if (btnExportarCaja) {
    btnExportarCaja.addEventListener('click', imprimirReporteCaja);
  }
  const btnExportarExcel = document.getElementById('btnExportarExcel');
  if (btnExportarExcel) {
    btnExportarExcel.addEventListener('click', exportarCajaExcel);
  }

  // Modal Nueva Cita
  document.getElementById('btnNuevaCita').addEventListener('click', () => {
    document.getElementById('inputFechaCita').value = fechaSeleccionada || new Date().toISOString().split('T')[0];
    if (filtroEstilista.value !== 'TODOS') {
      selectEstilista.value = filtroEstilista.value;
    }
    modalNuevaCita.classList.remove('hidden');
  });

  document.getElementById('btnCerrarModalNueva').addEventListener('click', cerrarModalNueva);
  document.getElementById('btnCancelarModalNueva').addEventListener('click', cerrarModalNueva);

  document.getElementById('btnCerrarModalReprogramar').addEventListener('click', cerrarModalReprogramar);
  document.getElementById('btnCancelarModalReprogramar').addEventListener('click', cerrarModalReprogramar);

  // Panel de Estadísticas con actualización en vivo sin recargar página
  btnToggleStats.addEventListener('click', () => {
    panelAdminStats.classList.toggle('hidden');
    if (!panelAdminStats.classList.contains('hidden')) {
      cargarEstadisticasAdmin();
      panelAdminStats.scrollIntoView({ behavior: 'smooth' });
      // Polling cada 4 segundos para mantener métricas sincronizadas en vivo
      if (!statsPollInterval) {
        statsPollInterval = setInterval(() => {
          if (!panelAdminStats.classList.contains('hidden')) {
            cargarEstadisticasAdmin();
          }
        }, 4000);
      }
    } else {
      if (statsPollInterval) {
        clearInterval(statsPollInterval);
        statsPollInterval = null;
      }
    }
  });

  btnCerrarStats.addEventListener('click', () => {
    panelAdminStats.classList.add('hidden');
    if (statsPollInterval) {
      clearInterval(statsPollInterval);
      statsPollInterval = null;
    }
  });
}

function desmarcarChips() {
  if (chipAyer) chipAyer.classList.remove('active');
  if (chipHoy) chipHoy.classList.remove('active');
  if (chipSemana) chipSemana.classList.remove('active');
  if (chipManana) chipManana.classList.remove('active');
  if (chipTodas) chipTodas.classList.remove('active');
}

function marcarChip(chip) {
  desmarcarChips();
  chip.classList.add('active');
}

function cerrarModalNueva() {
  modalNuevaCita.classList.add('hidden');
}

function cerrarModalReprogramar() {
  modalReprogramar.classList.add('hidden');
}

function mostrarToast(mensaje, tipo = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast toast-${tipo}`;
  
  const icono = tipo === 'success' ? '✓' : (tipo === 'error' ? '⚠️' : 'ℹ️');
  toast.innerHTML = `<span>${icono}</span> <span>${mensaje}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(30px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
