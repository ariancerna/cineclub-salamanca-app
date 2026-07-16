// Se deriva del host que sirve la pagina para que funcione igual en local y desplegado.
// Se puede forzar con window.CINECLUB_API_URL antes de cargar este script.
const API_PORT = '8080';
const BASE_URL = window.CINECLUB_API_URL
    || `${window.location.protocol}//${window.location.hostname}:${API_PORT}/api`;
const AUTH_KEY = 'cc_auth';

function getAuthData() {
    try { return JSON.parse(localStorage.getItem(AUTH_KEY)) || null; } catch { return null; }
}
function setAuthData(data)  { localStorage.setItem(AUTH_KEY, JSON.stringify(data)); }
function clearAuthData()    { localStorage.removeItem(AUTH_KEY); }

function normalizeApiError(err, fallback) {
    const rawMessage = err?.error || Object.values(err || {}).join(', ');
    if (/password|contrase/i.test(rawMessage) && /2147483647|tama/i.test(rawMessage)) {
        return 'La contraseña debe tener mínimo 8 caracteres';
    }
    return rawMessage || fallback;
}

function mapPelicula(p) {
    return {
        id:       p.id,
        titulo:   p.titulo,
        director: p.director,
        sinopsis: p.sinopsis,
        duracion: `${p.duracionMinutos} min`,
        img:      p.aficheUrl || ''
    };
}

function mapReserva(r) {
    return {
        id:        r.codigoReserva,
        peli:      r.funcion?.pelicula?.titulo || '',
        img:       r.funcion?.pelicula?.aficheUrl || '',
        user:      r.usuarioNombre,
        email:     r.usuarioEmail || '',
        seat:      r.numeroButaca,
        fecha:     r.fechaEmision ? new Date(r.fechaEmision).toLocaleString('es-PE') : '',
        funcionId: r.funcion?.id || null,
        confirmado: r.asistioIngreso || false,
        snacks:    (r.itemsMinibar || []).map(i => ({
            id:       i.productoId,
            nombre:   i.productoNombre,
            precio:   Number(i.subtotal) / i.cantidad,
            cantidad: i.cantidad
        }))
    };
}

const _funcionPorPelicula = {};

export const ApiService = {

    isLoggedIn()     { return !!getAuthData()?.token; },
    getCurrentUser() { return getAuthData(); },

    async login(email, password) {
        const res = await fetch(`${BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Credenciales incorrectas');
        }
        const data = await res.json();
        setAuthData(data);
        return data;
    },

    async register(nombre, email, password) {
        if (password.length < 8) throw new Error('La contraseña debe tener mínimo 8 caracteres');
        const res = await fetch(`${BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nombre, email, password })
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(normalizeApiError(err, 'Error al registrarse'));
        }
        const data = await res.json();
        setAuthData(data);
        return data;
    },

    logout() { clearAuthData(); },

    async getPeliculas() {
        const res = await fetch(`${BASE_URL}/peliculas`);
        if (!res.ok) throw new Error('Error al obtener películas');
        return (await res.json()).map(mapPelicula);
    },

    async getPeliculaById(id) {
        const res = await fetch(`${BASE_URL}/peliculas/${id}`);
        if (!res.ok) return null;
        return mapPelicula(await res.json());
    },

    async getFunciones() {
        const auth = getAuthData();
        const headers = auth?.token ? { 'Authorization': `Bearer ${auth.token}` } : {};
        const res = await fetch(`${BASE_URL}/funciones`, { headers });
        if (!res.ok) throw new Error('Error al obtener funciones');
        return await res.json();
    },

    async getFuncionByPeliculaId(peliculaId) {
        const funciones = await this.getFunciones();
        const funcion = funciones.find(f => String(f.pelicula?.id) === String(peliculaId));
        if (!funcion) return null;
        _funcionPorPelicula[peliculaId] = funcion.id;
        return funcion;
    },

    async getButacasOcupadas(peliculaId) {
        const funcion = await this.getFuncionByPeliculaId(peliculaId);
        if (!funcion) return [];
        const res = await fetch(`${BASE_URL}/funciones/${funcion.id}/butacas-ocupadas`);
        if (!res.ok) return [];
        return await res.json();
    },

    getFuncionId(peliculaId) { return _funcionPorPelicula[peliculaId] || null; },

    async crearReserva(reservaData) {
        const auth = getAuthData();
        if (!auth?.token) throw new Error('Debes iniciar sesión para reservar');

        let funcionId = _funcionPorPelicula[reservaData.movieId];
        if (!funcionId) {
            const funcion = await this.getFuncionByPeliculaId(reservaData.movieId);
            funcionId = funcion?.id || null;
        }
        if (!funcionId) throw new Error('No se encontró la función para esta película');

        const res = await fetch(`${BASE_URL}/reservas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${auth.token}` },
            body: JSON.stringify({
                funcionId,
                numeroButaca: reservaData.seat,
                itemsMinibar: (reservaData.snacks || []).map(s => ({ productoId: s.id, cantidad: s.cantidad }))
            })
        });
        if (!res.ok) {
            if (res.status === 401 || res.status === 403) {
                clearAuthData();
                throw new Error('Tu sesión ha expirado. Por favor vuelve a iniciar sesión.');
            }
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Error al crear la reserva');
        }
        const reservaNormalizada = mapReserva(await res.json());
        reservaNormalizada.snacks = reservaData.snacks || [];
        return { success: true, reserva: reservaNormalizada };
    },

    async getMisReservas() {
        const auth = getAuthData();
        if (!auth?.token) return [];
        const res = await fetch(`${BASE_URL}/reservas/mis-reservas`, {
            headers: { 'Authorization': `Bearer ${auth.token}` }
        });
        if (!res.ok) return [];
        return (await res.json()).map(mapReserva);
    },

    async getReservasPorFuncion(funcionId) {
        const auth = getAuthData();
        if (!auth?.token) return [];
        const res = await fetch(`${BASE_URL}/reservas/funcion/${funcionId}`, {
            headers: { 'Authorization': `Bearer ${auth.token}` }
        });
        if (!res.ok) return [];
        return (await res.json()).map(mapReserva);
    },

    async confirmarIngreso(codigo) {
        const auth = getAuthData();
        if (!auth?.token) throw new Error('Sin autorización');
        const res = await fetch(`${BASE_URL}/reservas/${codigo}/confirmar-ingreso`, {
            method: 'PATCH',
            headers: { 'Authorization': `Bearer ${auth.token}` }
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Error al confirmar ingreso');
        }
        return mapReserva(await res.json());
    },

    async getProductos() {
        const res = await fetch(`${BASE_URL}/productos`);
        if (!res.ok) throw new Error('Error al obtener productos');
        return (await res.json()).map(p => ({
            id:          p.id,
            nombre:      p.nombre,
            precio:      Number(p.precio),
            descripcion: p.descripcion,
            img:         p.imagenUrl || ''
        }));
    }
};
