import { getUltimaReserva } from '../utils/storage.js';
import { formatCurrency, parseSeat } from '../utils/helpers.js';

document.addEventListener('DOMContentLoaded', () => {
    if (window.lucide) lucide.createIcons();

    const reserva = getUltimaReserva();

    if (!reserva) {
        window.location.href = 'mis-entradas.html';
        return;
    }

    const { fila, numero } = parseSeat(reserva.seat);

    document.getElementById('ticket-user').innerText   = reserva.user  || '—';
    document.getElementById('ticket-title').innerText  = reserva.peli  || '—';
    document.getElementById('ticket-info').innerText   = reserva.fecha ? `Reservado el ${reserva.fecha}` : '';
    document.getElementById('ticket-fila').innerText   = fila   || '—';
    document.getElementById('ticket-butaca').innerText = numero || '—';
    document.getElementById('ticket-ref').innerText    = 'REF ID: ' + (reserva.id || '—');

    const snacksList   = document.getElementById('ticket-snacks');
    const totalDisplay = document.getElementById('ticket-total');
    let total = 0;

    if (reserva.snacks && reserva.snacks.length > 0) {
        snacksList.innerHTML = reserva.snacks.map(s => {
            total += s.precio * s.cantidad;
            return `
                <li class="flex justify-between">
                    <span><span class="font-bold">${s.cantidad}x</span> ${s.nombre}</span>
                    <span class="font-bold">${formatCurrency(s.precio * s.cantidad)}</span>
                </li>`;
        }).join('');
    } else {
        snacksList.innerHTML = '<li class="text-brand-outline italic text-[9px]">Sin consumos adicionales</li>';
    }

    if (totalDisplay) totalDisplay.innerText = formatCurrency(total);
});
