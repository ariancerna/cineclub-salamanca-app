/**
 * Ayudantes de localStorage
 * Centraliza todas las operaciones de persistencia local
 */

const KEYS = {
    RESERVAS:       'reservas',
    ULTIMA_RESERVA: 'ultima_reserva'
};

export function getReservas() {
    return JSON.parse(localStorage.getItem(KEYS.RESERVAS) || '[]');
}

export function saveReserva(reserva) {
    const reservas = getReservas();
    reservas.push(reserva);
    localStorage.setItem(KEYS.RESERVAS, JSON.stringify(reservas));
}

export function getUltimaReserva() {
    return JSON.parse(localStorage.getItem(KEYS.ULTIMA_RESERVA) || 'null');
}

export function setUltimaReserva(reserva) {
    localStorage.setItem(KEYS.ULTIMA_RESERVA, JSON.stringify(reserva));
}
