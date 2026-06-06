import { ApiService } from '../api.js';
import { parseSeat, formatCurrency } from '../utils/helpers.js';
import { setUltimaReserva } from '../utils/storage.js';

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const grid               = document.getElementById('seat-grid');
    const resumen            = document.getElementById('resumen-asiento');
    const btnConfirmar       = document.getElementById('btn-confirmar');
    const inputNombre        = document.getElementById('input-nombre');
    const modal              = document.getElementById('modal-snacks');
    const modalAuth          = document.getElementById('modal-auth');
    const snacksContainer    = document.getElementById('snacks-list');

    const peliculaId = new URLSearchParams(window.location.search).get('id');
    const pelicula   = await ApiService.getPeliculaById(peliculaId) || { titulo: 'Película' };
    const productos  = await ApiService.getProductos();
    const ocupados   = await ApiService.getButacasOcupadas(peliculaId);

    // Comprobar si el usuario ya tiene una reserva para la función correspondiente
    let tieneReservaParaFuncion = false;
    if (ApiService.isLoggedIn()) {
        const funcionId = ApiService.getFuncionId(peliculaId);
        if (funcionId) {
            const misReservas = await ApiService.getMisReservas();
            tieneReservaParaFuncion = !!misReservas.find(r => r.funcionId && String(r.funcionId) === String(funcionId));
        }
    }

    // Mostrar título de la película
    const tituloEl = document.getElementById('peli-titulo');
    if (tituloEl) tituloEl.textContent = pelicula.titulo;

    // Mostrar duración (si está disponible)
    const duracionEl = document.getElementById('peli-duracion');
    if (duracionEl) duracionEl.textContent = pelicula.duracion ? `Duración: ${pelicula.duracion}` : '';

    // Si ya está logueado, pre-rellenar el nombre
    const user = ApiService.getCurrentUser();
    if (user?.nombre && inputNombre) {
        inputNombre.value = user.nombre;
        inputNombre.readOnly = true;
        inputNombre.classList.add('text-brand-outline');
    }

    // Visualización del usuario en el menú: muestra el nombre y oculta "Acceder" si corresponde.
    const navUserEl = document.getElementById('nav-user');
    const navAuthEl = document.getElementById('nav-auth');
    const btnLogoutEl = document.getElementById('btn-logout');
    if (user?.nombre) {
        if (navUserEl) { navUserEl.textContent = user.nombre; navUserEl.classList.remove('hidden'); }
        if (btnLogoutEl) btnLogoutEl.classList.remove('hidden');
        if (navAuthEl) navAuthEl.classList.add('hidden');
    }

    let selectedSeat   = null;
    let seleccionSnacks = {};


    let authTab = 'login';

    window.switchTab = (tab) => {
        authTab = tab;
        document.getElementById('tab-login').className    = `flex-1 py-3 text-xs font-bold uppercase tracking-widest ${tab === 'login'    ? 'bg-brand-ink text-white' : 'hover:bg-gray-50'}`;
        document.getElementById('tab-registro').className = `flex-1 py-3 text-xs font-bold uppercase tracking-widest ${tab === 'registro' ? 'bg-brand-ink text-white' : 'hover:bg-gray-50'}`;
        document.getElementById('campo-nombre').classList.toggle('hidden', tab !== 'registro');
        document.getElementById('auth-error').classList.add('hidden');
    };

    window.submitAuth = async () => {
        const email    = document.getElementById('auth-email').value.trim();
        const password = document.getElementById('auth-password').value;
        const nombre   = document.getElementById('auth-nombre').value.trim();
        const errEl    = document.getElementById('auth-error');
        errEl.classList.add('hidden');

        try {
            if (authTab === 'login') {
                await ApiService.login(email, password);
            } else {
                if (!nombre) { errEl.textContent = 'El nombre es obligatorio'; errEl.classList.remove('hidden'); return; }
                await ApiService.register(nombre, email, password);
            }
            // Autenticación correcta: vuelve a evaluar si el usuario ya tiene una reserva para esta función.
            // Cerrar el modal de autenticación primero
            modalAuth.classList.replace('flex', 'hidden');
            // Asegurar que el scroll del body esté habilitado de nuevo
            try { document.body.style.overflow = ''; } catch (e) { /* noop */ }

            // Autocompletar nombre si corresponde
            const afterUser = ApiService.getCurrentUser();
            if (afterUser?.nombre) {
                inputNombre.value = afterUser.nombre;
                inputNombre.readOnly = true;
                inputNombre.classList.add('text-brand-outline');
                // Actualizar nav: mostrar nombre y ocultar 'Acceder'
                if (navUserEl) { navUserEl.textContent = afterUser.nombre; navUserEl.classList.remove('hidden'); }
                if (btnLogoutEl) btnLogoutEl.classList.remove('hidden');
                if (navAuthEl) navAuthEl.classList.add('hidden');
            }

            // Asegurar que el funcionId sea conocido
            const funcionObjAfterAuth = await ApiService.getFuncionByPeliculaId(peliculaId);
            const funcionIdAfterAuth = funcionObjAfterAuth?.id;

            let tieneReservaAhora = false;
            if (funcionIdAfterAuth) {
                const misReservas = await ApiService.getMisReservas();
                tieneReservaAhora = !!misReservas.find(r => r.funcionId && String(r.funcionId) === String(funcionIdAfterAuth));
            }

            if (tieneReservaAhora) {
                // Actualizar estado e interfaz, mostrar modal informativo descartable
                tieneReservaParaFuncion = true;
                updateUI();
                showExistingReservationModal();
            } else {
                // Sin reserva previa → abrir modal de snacks
                tieneReservaParaFuncion = false;
                updateUI();
                modal.classList.remove('hidden');
                modal.classList.add('flex');
                document.body.style.overflow = 'hidden';
            }
        } catch (e) {
            errEl.textContent = e.message;
            errEl.classList.remove('hidden');
        }
    };

    function showExistingReservationModal() {
        // Si el modal ya existe, no recrearlo
        if (document.getElementById('modal-reserva-existente')) return;
        const modalEx = document.createElement('div');
        modalEx.id = 'modal-reserva-existente';
        modalEx.className = 'fixed inset-0 z-50 flex items-center justify-center p-6 h-screen';
        modalEx.innerHTML = `
            <div class="absolute inset-0 bg-brand-ink/90"></div>
            <div class="relative bg-white w-full max-w-md border border-brand-ink shadow-hard p-6">
                <button id="modal-res-cerrar" class="absolute top-3 right-3 text-brand-outline font-bold">✕</button>
                <h3 class="text-xl font-bold mb-3">Ya tienes una reserva para esta función</h3>
                <p class="text-sm text-brand-outline mb-6">Puedes ver los detalles de tu reserva, pero no puedes reservar otra vez para la misma función.</p>
                <div class="flex gap-4 justify-end">
                    <a href="mis-entradas.html" class="btn-outline">Ver mis entradas</a>
                    <button id="modal-res-cerrar-2" class="btn-primary">Cerrar</button>
                </div>
            </div>
        `;
        document.body.appendChild(modalEx);

        // Bloquear scroll del body mientras el modal esté presente
        document.body.style.overflow = 'hidden';

        const escHandlerEx = (ev) => { if (ev.key === 'Escape') removerModal(); };

        const cerrar = document.getElementById('modal-res-cerrar');
        const cerrar2 = document.getElementById('modal-res-cerrar-2');
        const removerModal = () => { modalEx.remove(); document.body.style.overflow = ''; document.removeEventListener('keydown', escHandlerEx); };
        cerrar?.addEventListener('click', removerModal);
        cerrar2?.addEventListener('click', removerModal);
        document.addEventListener('keydown', escHandlerEx);
    }

    // Renderizar asientos

    function renderSeats() {
        const rows = ['A', 'B', 'C', 'D'];
        grid.innerHTML = rows.map(fila => `
            <div class="flex gap-3 min-w-max">
                <span class="w-6 font-bold text-brand-outline uppercase text-[10px] flex items-center justify-end">${fila}</span>
                <div class="flex gap-2">
                    ${[1,2,3,4,5,6,7,8].map(n => {
                        const id    = `${fila}-${n}`;
                        const isOcc = ocupados.includes(id);
                        const isSel = selectedSeat === id;
                        return `
                            <button data-seat="${id}"
                                class="w-8 h-8 md:w-10 md:h-10 border text-[10px] md:text-xs flex items-center justify-center transition-all
                                    ${isOcc ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                            : (isSel ? 'bg-brand-primary text-white border-brand-primary shadow-hard scale-110'
                                                     : 'bg-white border-brand-ink hover:border-brand-primary')}"
                                ${isOcc ? 'disabled' : ''}>
                                ${isOcc ? '×' : id}
                            </button>
                        `;
                    }).join('')}
                </div>
            </div>
        `).join('');

        grid.querySelectorAll('button[data-seat]').forEach(btn => {
            btn.onclick = () => {
                const id  = btn.getAttribute('data-seat');
                selectedSeat = (selectedSeat === id) ? null : id;
                updateUI();
                renderSeats();
            };
        });
    }

    // Renderizar snacks

    function renderSnacks() {
        snacksContainer.innerHTML = productos.map(p => `
            <div class="flex flex-col sm:flex-row gap-4 sm:gap-6 p-4 border border-brand-ink bg-white">
                <div class="w-full sm:w-20 h-32 sm:h-20 bg-gray-100 overflow-hidden">
                    <img src="${p.img}" class="w-full h-full object-cover grayscale" alt="${p.nombre}">
                </div>
                <div class="flex-grow">
                    <div class="flex justify-between items-start">
                        <h4 class="font-bold uppercase text-sm">${p.nombre}</h4>
                        <span class="font-bold text-sm">${formatCurrency(p.precio)}</span>
                    </div>
                    <p class="text-[10px] text-brand-outline mb-4 sm:mb-2">${p.descripcion}</p>
                    <div class="flex justify-end items-center mt-auto">
                        <div class="flex items-center border border-brand-ink px-1 bg-white">
                            <button class="p-2 hover:text-brand-primary transition-colors" onclick="window.updateSnack(${p.id}, -1)">
                                <i data-lucide="minus" class="w-3 h-3"></i>
                            </button>
                            <span class="w-10 text-center text-xs font-bold">${seleccionSnacks[p.id] || 0}</span>
                            <button class="p-2 hover:text-brand-primary transition-colors" onclick="window.updateSnack(${p.id}, 1)">
                                <i data-lucide="plus" class="w-3 h-3"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');

        const total = productos.reduce((acc, p) => acc + (p.precio * (seleccionSnacks[p.id] || 0)), 0);
        const totalDisplay = document.getElementById('snacks-total');
        if (totalDisplay) totalDisplay.innerText = formatCurrency(total);

        if (window.lucide) lucide.createIcons();
    }

    window.updateSnack = (id, delta) => {
        seleccionSnacks[id] = Math.max(0, (seleccionSnacks[id] || 0) + delta);
        renderSnacks();
    };

    window.closeSnacksModal = () => {
        modal.classList.remove('flex');
        modal.classList.add('hidden');
        document.body.style.overflow = '';
    };

    // Ayudantes de interfaz

    function updateUI() {
        if (selectedSeat) {
            const { fila, numero } = parseSeat(selectedSeat);
            resumen.innerHTML = `Fila ${fila}, Asiento ${numero}`;
            resumen.classList.remove('text-brand-outline', 'italic');
        } else {
            resumen.innerHTML = 'Ninguno seleccionado';
            resumen.classList.add('text-brand-outline', 'italic');
        }
        // No permitir continuar si el usuario ya tiene reserva para esta función
        const disabled = !!tieneReservaParaFuncion || !selectedSeat || !inputNombre.value.trim();
        btnConfirmar.disabled = disabled;

        // Ajustar apariencia cuando está deshabilitado: mantener tamaño (padding) pero atenuar colores
        if (disabled) {
            // Mantener 'btn-primary' para conservar padding/estilo, solo atenuar visualmente
            btnConfirmar.classList.add('opacity-50', 'cursor-not-allowed');
            btnConfirmar.classList.remove('shadow-hard', 'shadow-hard-active');
            btnConfirmar.style.backgroundColor = '';
            btnConfirmar.classList.add('bg-gray-200', 'text-gray-500');
            btnConfirmar.setAttribute('aria-disabled', 'true');
        } else {
            btnConfirmar.classList.add('btn-primary');
            btnConfirmar.classList.add('shadow-hard', 'shadow-hard-active');
            btnConfirmar.classList.remove('opacity-50', 'cursor-not-allowed', 'bg-gray-200', 'text-gray-500');
            btnConfirmar.removeAttribute('aria-disabled');
            btnConfirmar.style.backgroundColor = '';
        }
    }

    inputNombre.addEventListener('input', updateUI);

    btnConfirmar.onclick = () => {
        if (!ApiService.isLoggedIn()) {
            // Prellena el nombre en el formulario de registro si el usuario escribió uno.
            const nombre = inputNombre.value.trim();
            if (nombre) document.getElementById('auth-nombre').value = nombre;
            modalAuth.classList.remove('hidden');
            modalAuth.classList.add('flex');
            document.body.style.overflow = 'hidden';
        } else {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
            document.body.style.overflow = 'hidden';
        }
    };

    // Finalizar reserva

    window.finalizarReserva = async () => {
        const snacks = productos
            .filter(p => seleccionSnacks[p.id] > 0)
            .map(p => ({ ...p, cantidad: seleccionSnacks[p.id] }));

        try {
            const result = await ApiService.crearReserva({
                movieId: peliculaId,
                seat:    selectedSeat,
                snacks
            });

            // Normaliza para detalle-ticket (agrega datos extra del frontend)
            const reservaLocal = {
                ...result.reserva,
                peli:  pelicula.titulo,
                img:   pelicula.img,
                snacks
            };

            setUltimaReserva(reservaLocal);
            window.location.href = 'detalle-ticket.html';
        } catch (e) {
            alert(e.message);
        }
    };

    renderSeats();
    renderSnacks();
    // Ajusta la interfaz inicial según el estado del botón Continuar.
    updateUI();

    // Actualizar meta (sala y fecha/hora) usando la función encontrada para esta película
    const metaEl = document.getElementById('peli-meta');
    const funcionObj = await ApiService.getFuncionByPeliculaId(peliculaId);
    if (metaEl) {
        if (funcionObj) {
            const fecha = new Date(funcionObj.fechaHora);
            const fechaStr = fecha.toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' });
            const horaStr = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
            metaEl.textContent = `${funcionObj.sala ? funcionObj.sala + ' • ' : ''}${fechaStr} • ${horaStr}`;
        } else {
            metaEl.textContent = '';
        }
    }

    // Experiencia para usuario con reserva existente (Opción B)
    if (tieneReservaParaFuncion) {
        // Actualiza la interfaz para reflejar el estado deshabilitado.
        updateUI();

        // Añade un enlace visible en la barra lateral para ver las entradas.
        const resumenContainer = document.querySelector('.lg\\:col-span-4 .space-y-8');
        if (resumenContainer) {
            const verLink = document.createElement('a');
            verLink.href = 'mis-entradas.html';
            verLink.className = 'block text-xs font-bold uppercase text-center mt-4 underline';
            verLink.textContent = 'Ya tienes una reserva • Ver mis entradas';
            resumenContainer.prepend(verLink);
        }

        showExistingReservationModal();
    }

    // Enlace de autenticación del menú superior: abre el modal.
    const navAuth = document.getElementById('nav-auth');
    if (navAuth) {
        const openAuth = () => {
            window.scrollTo(0, 0);
            modalAuth.classList.remove('hidden');
            modalAuth.classList.add('flex');
            document.body.style.overflow = 'hidden';
            const u = ApiService.getCurrentUser();
            if (u?.nombre) {
                document.getElementById('auth-nombre').value = u.nombre;
            }
            document.getElementById('auth-email')?.focus();
            document.addEventListener('keydown', escHandler);
        };

        const closeAuth = () => {
            if (modalAuth.classList.contains('flex')) modalAuth.classList.replace('flex', 'hidden');
            document.body.style.overflow = '';
            document.removeEventListener('keydown', escHandler);
        };

        const escHandler = (ev) => { if (ev.key === 'Escape') closeAuth(); };

        navAuth.addEventListener('click', (ev) => { ev.preventDefault(); openAuth(); });
        document.getElementById('modal-auth-close')?.addEventListener('click', closeAuth);
    }

    // Manejador de cierre de sesión para esta página.
    window.doLogout = () => { ApiService.logout(); location.reload(); };
});
