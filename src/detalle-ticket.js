/**
 * Lógica para el Detalle del Ticket
 */

document.addEventListener('DOMContentLoaded', () => {
    if (window.lucide) lucide.createIcons();

    const reserva = JSON.parse(localStorage.getItem('ultima_reserva'));
    if (reserva) {
        document.getElementById('ticket-user').innerText = reserva.user;
        document.getElementById('ticket-title').innerText = reserva.peli;
        document.getElementById('ticket-info').innerText = `Función: ${reserva.fecha.split(',')[0]} (Sesión Hoy)`;
        document.getElementById('ticket-fila').innerText = reserva.seat.split('-')[0];
        document.getElementById('ticket-butaca').innerText = reserva.seat.split('-')[1];
        document.getElementById('ticket-ref').innerText = 'REF ID: ' + reserva.id;
        
        const snacksList = document.getElementById('ticket-snacks');
        const totalDisplay = document.getElementById('ticket-total');
        
        let total = 0;
        if (reserva.snacks && reserva.snacks.length > 0) {
            snacksList.innerHTML = reserva.snacks.map(s => {
                total += s.precio * s.cantidad;
                return `
                    <li class="flex justify-between">
                        <span><span class="font-bold">${s.cantidad}x</span> ${s.nombre}</span>
                        <span>Confirmado</span>
                    </li>
                `;
            }).join('');
        } else {
            snacksList.innerHTML = '<li class="text-brand-outline italic text-[8px]">Sin consumos adicionales</li>';
        }
        
        if (totalDisplay) {
            totalDisplay.innerText = `S/ ${total.toFixed(2)}`;
        }
    }
});
