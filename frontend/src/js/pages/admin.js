import { ApiService } from '../api.js';

let todasLasReservas = [];

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const user = ApiService.getCurrentUser();

    if (!user?.token) {
        window.location.href = 'index.html';
        return;
    }

    setupLogout(user);

    if (user.rol !== 'ROLE_ADMIN') {
        mostrarAccesoDenegado();
        return;
    }

    try {
        await cargarFunciones();
    } catch (err) {
        mostrarError('Error inesperado: ' + err.message);
    }

    setupBusqueda();
});

function mostrarAccesoDenegado() {
    document.getElementById('admin-table').innerHTML = `
        <div class="p-12 text-center space-y-4">
            <p class="text-brand-primary font-bold text-lg uppercase tracking-widest">Acceso restringido</p>
            <p class="text-brand-outline text-sm">Esta sección es solo para administradores.</p>
            <a href="index.html" class="inline-block mt-4 text-xs font-bold uppercase underline hover:text-brand-primary">Volver al inicio</a>
        </div>`;

    const select = document.getElementById('select-funcion');
    if (select) select.innerHTML = '<option value="">Sin acceso</option>';
}

async function cargarFunciones() {
    const select = document.getElementById('select-funcion');

    let funciones = [];
    try {
        funciones = await ApiService.getFunciones();
    } catch {
        mostrarError('No se pudo conectar con el servidor. Verifica que el backend esté corriendo.');
        return;
    }

    if (!funciones || funciones.length === 0) {
        select.innerHTML = '<option value="">Sin funciones</option>';
        mostrarVacio('No hay funciones registradas aún.');
        return;
    }

    funciones.sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora));

    select.innerHTML = funciones.map(f => {
        const fecha = new Date(f.fechaHora).toLocaleString('es-PE', {
            day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
        });
        const titulo = f.pelicula?.titulo || `Función #${f.id}`;
        return `<option value="${f.id}">${titulo} — ${fecha}</option>`;
    }).join('');

    const ahora = new Date();
    const proxima = funciones.find(f => new Date(f.fechaHora) >= ahora) || funciones[funciones.length - 1];

    if (!proxima) {
        mostrarVacio('No hay funciones disponibles.');
        return;
    }

    select.value = String(proxima.id);
    await cargarReservas(proxima);

    select.addEventListener('change', async () => {
        const seleccionada = funciones.find(f => String(f.id) === select.value);
        if (seleccionada) await cargarReservas(seleccionada);
    });
}

async function cargarReservas(funcion) {
    const sesionEl = document.getElementById('sesion-info');
    if (sesionEl) {
        const fecha = new Date(funcion.fechaHora).toLocaleString('es-PE', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
        sesionEl.textContent = `${funcion.pelicula?.titulo || 'Función'} — ${fecha}`;
    }

    mostrarCargando();

    try {
        todasLasReservas = await ApiService.getReservasPorFuncion(funcion.id);
        renderTabla(todasLasReservas);
        actualizarStats();
    } catch (err) {
        mostrarError('Error al cargar reservas: ' + err.message);
    }
}

function renderTabla(reservas) {
    const table = document.getElementById('admin-table');

    if (reservas.length === 0) {
        table.innerHTML = `
            <div class="p-12 text-center border-2 border-dashed border-brand-outline m-6">
                <p class="text-brand-outline font-bold text-sm italic">No hay reservas para esta función.</p>
            </div>`;
        return;
    }

    table.innerHTML = reservas.map(r => {
        const iniciales = r.user?.split(' ').slice(0, 2).map(n => n[0]?.toUpperCase()).join('') || '?';
        return `
            <div class="grid grid-cols-12 items-center py-5 px-6 hover:bg-gray-50 transition-colors" data-codigo="${r.id}">
                <div class="col-span-3 font-mono text-xs text-brand-outline">${r.id}</div>
                <div class="col-span-1 font-bold text-sm">${r.seat}</div>
                <div class="col-span-4 flex gap-3 items-center">
                    <div class="w-9 h-9 bg-brand-primary/10 text-brand-primary flex items-center justify-center rounded-full font-bold text-[10px] shrink-0">${iniciales}</div>
                    <div>
                        <div class="font-bold text-sm leading-tight">${r.user}</div>
                        <div class="text-[9px] font-bold text-brand-outline tracking-wider">${r.email || 'Espectador'}</div>
                    </div>
                </div>
                <div class="col-span-2">
                    ${r.confirmado
                        ? `<span class="text-[9px] font-bold uppercase px-2 py-1 bg-green-50 border border-green-200 text-green-700">INGRESÓ</span>`
                        : `<span class="text-[9px] font-bold uppercase px-2 py-1 border border-brand-outline text-brand-outline">PENDIENTE</span>`
                    }
                </div>
                <div class="col-span-2 flex justify-end">
                    <button
                        onclick="window.confirmarIngreso('${r.id}', this)"
                        title="${r.confirmado ? 'Ya ingresó' : 'Confirmar ingreso'}"
                        ${r.confirmado ? 'disabled' : ''}
                        class="w-10 h-10 border flex items-center justify-center transition-all
                            ${r.confirmado
                                ? 'border-green-300 bg-green-50 text-green-600 cursor-default'
                                : 'border-brand-ink hover:bg-brand-ink hover:text-white cursor-pointer'
                            }">
                        <i data-lucide="check" class="w-4 h-4"></i>
                    </button>
                </div>
            </div>`;
    }).join('');

    if (window.lucide) lucide.createIcons();
}

function actualizarStats() {
    const total      = todasLasReservas.length;
    const ingresados = todasLasReservas.filter(r => r.confirmado).length;
    const pendientes = total - ingresados;
    const el = id => document.getElementById(id);
    if (el('stat-total'))      el('stat-total').textContent      = total;
    if (el('stat-ingresados')) el('stat-ingresados').textContent = ingresados;
    if (el('stat-pendientes')) el('stat-pendientes').textContent = pendientes;
}

function mostrarCargando() {
    document.getElementById('admin-table').innerHTML = `
        <div class="p-12 text-center">
            <div class="w-6 h-6 border-2 border-brand-primary border-t-transparent rounded-full spin mx-auto mb-3"></div>
            <p class="text-brand-outline font-bold text-xs uppercase tracking-widest">Cargando reservas...</p>
        </div>`;
}

function mostrarError(mensaje) {
    document.getElementById('admin-table').innerHTML = `
        <div class="p-12 text-center">
            <p class="text-brand-primary font-bold text-sm">${mensaje}</p>
        </div>`;
}

function mostrarVacio(mensaje) {
    document.getElementById('admin-table').innerHTML = `
        <div class="p-12 text-center">
            <p class="text-brand-outline font-bold text-sm italic">${mensaje}</p>
        </div>`;
}

function setupBusqueda() {
    const input = document.getElementById('search-input');
    const btn   = document.getElementById('search-btn');

    const filtrar = () => {
        const q = input?.value.trim().toLowerCase() || '';
        if (!q) { renderTabla(todasLasReservas); actualizarStats(); return; }
        const filtradas = todasLasReservas.filter(r =>
            r.id?.toLowerCase().includes(q) ||
            r.user?.toLowerCase().includes(q) ||
            r.email?.toLowerCase().includes(q)
        );
        renderTabla(filtradas);
        const el = id => document.getElementById(id);
        if (el('stat-total')) el('stat-total').textContent = filtradas.length;
    };

    btn?.addEventListener('click', filtrar);
    input?.addEventListener('keydown', e => { if (e.key === 'Enter') filtrar(); });
    input?.addEventListener('input',   () => { if (!input.value) { renderTabla(todasLasReservas); actualizarStats(); } });
}

function setupLogout(user) {
    const btn = document.getElementById('nav-logout');
    if (btn) {
        btn.textContent = user?.nombre ? `Salir (${user.nombre})` : 'Salir';
        btn.addEventListener('click', () => { ApiService.logout(); window.location.href = 'index.html'; });
    }
}

window.confirmarIngreso = async (codigo, btn) => {
    if (btn.disabled) return;
    btn.disabled = true;
    btn.innerHTML = '<div class="w-4 h-4 border-2 border-current border-t-transparent rounded-full spin"></div>';

    try {
        await ApiService.confirmarIngreso(codigo);

        const reserva = todasLasReservas.find(r => r.id === codigo);
        if (reserva) reserva.confirmado = true;

        const row = document.querySelector(`[data-codigo="${codigo}"]`);
        if (row) {
            const estadoEl = row.querySelector('.col-span-2:nth-child(5)');
            if (estadoEl) estadoEl.innerHTML = `<span class="text-[9px] font-bold uppercase px-2 py-1 bg-green-50 border border-green-200 text-green-700">INGRESÓ</span>`;
            btn.className = 'w-10 h-10 border border-green-300 bg-green-50 text-green-600 cursor-default flex items-center justify-center';
            btn.innerHTML = '<i data-lucide="check" class="w-4 h-4"></i>';
            if (window.lucide) lucide.createIcons();
        }

        actualizarStats();
    } catch (err) {
        btn.disabled = false;
        btn.innerHTML = '<i data-lucide="check" class="w-4 h-4"></i>';
        if (window.lucide) lucide.createIcons();
        alert('Error: ' + err.message);
    }
};
