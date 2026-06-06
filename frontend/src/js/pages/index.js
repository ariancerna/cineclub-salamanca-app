import { ApiService } from '../api.js';

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const current = ApiService.getCurrentUser();
    const navUser = document.getElementById('nav-user');
    const btnLogout = document.getElementById('btn-logout');
    const navAuth = document.getElementById('nav-auth');
    if (current?.nombre) {
        if (navUser) { navUser.textContent = current.nombre; navUser.classList.remove('hidden'); }
        if (btnLogout) btnLogout.classList.remove('hidden');
        if (navAuth) navAuth.classList.add('hidden');
    }
    window.doLogout = () => { ApiService.logout(); location.reload(); };

    const grid = document.getElementById('cartelera-grid');
    const header = document.querySelector('header');
    const peliculas = await ApiService.getPeliculas();
    const funciones = await ApiService.getFunciones().catch(() => []);
    const salaMap = {};
    (funciones || []).forEach(f => {
        if (f?.pelicula?.id) salaMap[String(f.pelicula.id)] = f.sala;
    });

    if (header && peliculas.length > 0) {
        const featured = peliculas[0];
        header.innerHTML = `
            <div class="max-w-7xl mx-auto px-6 py-20 flex flex-col md:flex-row gap-12 items-center">
                <div class="w-full md:w-3/5 aspect-video bg-gray-900 border border-white/10 overflow-hidden">
                    <img src="${featured.img}" alt="${featured.titulo}" class="w-full h-full object-cover grayscale opacity-80">
                    <div class="absolute top-4 left-4 bg-brand-ink text-white text-[8px] font-bold uppercase py-1 px-3 tracking-widest z-10">
                        ${featured.duracion}
                    </div>
                </div>
                <div class="w-full md:w-2/5 flex flex-col items-start gap-6">
                    <span class="border border-brand-primary text-brand-primary px-3 py-1 text-xs font-bold uppercase tracking-widest leading-none">Película Destacada</span>
                    <h1 class="text-5xl md:text-6xl font-bold uppercase leading-[0.9] tracking-tighter">${featured.titulo}</h1>
                            <p class="text-sm text-brand-outline">${salaMap[String(featured.id)] ? 'Sala: ' + salaMap[String(featured.id)] : ''}</p>
                            <p class="text-gray-400 border-l-2 border-brand-primary pl-4 text-base md:text-lg">
                                ${featured.sinopsis}
                            </p>
                    <a href="reserva.html?id=${featured.id}" class="btn-primary w-full md:w-auto text-center">Reservar ahora</a>
                </div>
            </div>
        `;
    }

    if (grid) {
        const rest = peliculas.slice(1);
        if (rest.length === 0) {
            grid.innerHTML = '<p class="col-span-full text-center text-gray-500">No hay películas disponibles</p>';
        } else {
            grid.innerHTML = rest.map(p => {
                const sala = salaMap[String(p.id)] || '';
                return `
                <div class="group border border-brand-outline bg-white overflow-hidden hover:-translate-y-2 transition-all flex flex-col h-full shadow-hard-active">
                    <div class="aspect-[4/3] bg-gray-100 overflow-hidden grayscale group-hover:grayscale-0 transition-all duration-500 relative">
                        <img src="${p.img}" class="w-full h-full object-cover" alt="${p.titulo}">
                        <div class="absolute top-4 left-4 bg-brand-ink text-white text-[8px] font-bold uppercase py-1 px-3 tracking-widest z-10">
                            ${p.duracion}
                        </div>
                    </div>
                    <div class="p-6 md:p-8 flex flex-col flex-grow">
                        <h3 class="text-2xl font-bold mb-1 uppercase tracking-tight">${p.titulo}</h3>
                        <p class="text-brand-outline text-[10px] font-bold uppercase mb-2 tracking-widest">Dir. ${p.director} ${sala ? '• ' + sala : ''}</p>
                        <p class="text-xs md:text-sm text-gray-600 line-clamp-3 mb-8 flex-grow leading-relaxed">
                            ${p.sinopsis}
                        </p>
                        <a href="reserva.html?id=${p.id}" class="w-full inline-block text-center btn-outline border-brand-outline hover:border-brand-primary text-xs py-3">
                            Reservar Butaca Gratis
                        </a>
                    </div>
                </div>
            `;
        }).join('');
        }
    }

    if (window.lucide) lucide.createIcons();

    // Manejador de autenticación del menú, si el modal existe.
    const modalAuth = document.getElementById('modal-auth');
    if (navAuth && modalAuth) {
        const openAuth = () => {
            window.scrollTo(0, 0);
            modalAuth.classList.remove('hidden');
            modalAuth.classList.add('flex');
            document.body.style.overflow = 'hidden';
            document.getElementById('campo-nombre')?.classList.add('hidden');
            document.getElementById('auth-error')?.classList.add('hidden');
            document.getElementById('auth-email')?.focus();
            // Cierra el modal al presionar Escape.
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

        // Cambia entre iniciar sesión y registrarse, luego recarga al autenticar.
        window.switchTab = (tab) => {
            document.getElementById('tab-login').className = `flex-1 py-3 text-xs font-bold uppercase tracking-widest ${tab === 'login' ? 'bg-brand-ink text-white' : 'hover:bg-gray-50'}`;
            document.getElementById('tab-registro').className = `flex-1 py-3 text-xs font-bold uppercase tracking-widest ${tab === 'registro' ? 'bg-brand-ink text-white' : 'hover:bg-gray-50'}`;
            document.getElementById('campo-nombre').classList.toggle('hidden', tab !== 'registro');
            document.getElementById('auth-error')?.classList.add('hidden');
        };

        window.submitAuth = async () => {
            const email = document.getElementById('auth-email').value.trim();
            const password = document.getElementById('auth-password').value;
            const nombre = document.getElementById('auth-nombre').value.trim();
            const errEl = document.getElementById('auth-error');
            errEl.classList.add('hidden');
            try {
                const isRegister = document.getElementById('campo-nombre') && !document.getElementById('campo-nombre').classList.contains('hidden');
                if (isRegister) {
                    if (!nombre) { errEl.textContent = 'El nombre es obligatorio'; errEl.classList.remove('hidden'); return; }
                    await ApiService.register(nombre, email, password);
                } else {
                    await ApiService.login(email, password);
                }
                closeAuth();
                location.reload();
            } catch (e) {
                errEl.textContent = e.message; errEl.classList.remove('hidden');
            }
        };
    }
});
