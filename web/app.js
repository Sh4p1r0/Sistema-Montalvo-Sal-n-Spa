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
let currentUser = null; // Recepcionista autenticada
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

// Inicialización de la aplicación
document.addEventListener('DOMContentLoaded', async () => {
  inputFechaFiltro.value = fechaSeleccionada;
  document.getElementById('inputFechaCita').value = fechaSeleccionada;
  document.getElementById('reprogramNuevaFecha').value = fechaSeleccionada;

  await cargarServicios();
  await cargarEstilistas();

  configurarAutenticacion();
  configurarEventos();
});

// ==========================================================
// 1. SISTEMA DE AUTENTICACIÓN Y ACCESO (PATRÓN PROXY)
// ==========================================================
function configurarAutenticacion() {
  const guardado = localStorage.getItem('montalvo_user');
  if (guardado) {
    try {
      currentUser = JSON.parse(guardado);
      aplicarPerfilUsuario();
    } catch (e) {
      currentUser = null;
    }
  }

  if (!currentUser) {
    // Sesión por defecto de Recepcionista para agilizar uso
    establecerSesion({
      id: 1,
      username: 'recep',
      rol: 'RECEPCIONISTA',
      nombre: 'Recepción Principal',
      areaServicio: 'Caja y Atención General'
    });
  } else {
    cargarCitas();
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
      if (data.success && data.usuario) {
        establecerSesion(data.usuario);
      } else {
        mostrarErrorLogin(data.error || 'Credenciales inválidas.');
      }
    } catch (err) {
      // Fallback local si no hay conexión
      console.warn('Fallback login offline:', err);
      if (u === 'recep' && p === 'montalvo') {
        establecerSesion({ id: 1, username: 'recep', rol: 'RECEPCIONISTA', nombre: 'Recepción Principal', areaServicio: 'Caja y Atención General' });
      } else if (u === 'admin' && p === 'montalvo123') {
        establecerSesion({ id: 2, username: 'admin', rol: 'RECEPCIONISTA', nombre: 'Administración Montalvo', areaServicio: 'Gerencia' });
      } else {
        mostrarErrorLogin('Credenciales inválidas. Usa el botón de Acceso Rápido.');
      }
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

function establecerSesion(user) {
  currentUser = user;
  localStorage.setItem('montalvo_user', JSON.stringify(user));
  cerrarModalLogin();
  aplicarPerfilUsuario();
  cargarCitas();
  mostrarToast(`¡Sesión activa: ${user.nombre}!`, 'success');
}

function cerrarSesion() {
  currentUser = null;
  localStorage.removeItem('montalvo_user');
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
  userRoleTag.textContent = 'RECEPCIÓN • CONTROL TOTAL';

  btnToggleStats.classList.remove('hidden');
  groupFiltroEstilista.classList.remove('hidden');
}

// ==========================================================
// 2. CARGA DE CATÁLOGOS (FACTORY & PROXY)
// ==========================================================
async function cargarServicios() {
  try {
    const res = await fetch(`${API_BASE}/api/servicios`);
    servicios = await res.json();
    
    selectServicio.innerHTML = servicios.map(s => `
      <option value="${s.codigo}" data-precio="${s.precio}" data-duracion="${s.duracion}">
        ${s.nombre} - S/. ${s.precio.toFixed(2)} (${s.duracion} min)
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
        ${s.nombre} - S/. ${s.precio.toFixed(2)} (${s.duracion} min)
      </option>
    `).join('');
    actualizarPreviewServicio();
  }
}

async function cargarEstilistas() {
  try {
    const res = await fetch(`${API_BASE}/api/estilistas`);
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

    const res = await fetch(url);
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
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#d4af37" stroke-width="2.2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
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
    const res = await fetch(`${API_BASE}/api/citas/finalizar`, {
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
    const res = await fetch(`${API_BASE}/api/citas/estado`, {
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
    const res = await fetch(`${API_BASE}/api/citas`, {
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
    const res = await fetch(`${API_BASE}/api/citas/reprogramar`, {
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
    const res = await fetch(`${API_BASE}/api/stats`);
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

  // Actualizar Caja de Recepción en Tiempo Real
  try {
    const res = await fetch(`${API_BASE}/api/caja?periodo=${periodoSeleccionado}&fecha=${fechaSeleccionada}`);
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

async function abrirModalCaja() {
  try {
    const res = await fetch(`${API_BASE}/api/caja?periodo=${periodoSeleccionado}&fecha=${fechaSeleccionada}`);
    if (!res.ok) throw new Error('No se pudo cargar los datos de caja.');
    const data = await res.json();

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
          <td style="color:var(--gold-primary);font-weight:700;">S/. ${c.precio.toFixed(2)}</td>
          <td>
            <span class="status-pill ${c.estado === 'Finalizada' ? 'canceled' : 'active'}" style="font-size:0.65rem;padding:0.15rem 0.5rem;">
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
    btnExportarCaja.addEventListener('click', () => {
      window.print();
    });
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
