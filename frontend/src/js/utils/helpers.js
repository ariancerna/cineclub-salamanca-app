/**
 * Funciones de utilidad general
 */

export function generateId() {
    return Math.floor(Math.random() * 80000) + 10000;
}

export function formatCurrency(amount) {
    return `S/ ${parseFloat(amount).toFixed(2)}`;
}

export function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('es-PE', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

/**
 * Parsea un ID de butaca "A-3" y devuelve { fila: "A", numero: "3" }.
 */
export function parseSeat(seatId) {
    const [fila, numero] = seatId.split('-');
    return { fila, numero };
}
