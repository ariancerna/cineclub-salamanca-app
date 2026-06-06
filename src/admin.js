/**
 * Lógica para la página de Administración (admin.html)
 */

import { ApiService } from './api.js';

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const table = document.getElementById('admin-table');
    const reservas = await ApiService.getAllBookings();
    console.log("Reservas:", reservas);
    
    if (table) {
        table.addEventListener('click', (e) => {
            const btn = e.target.closest('button');
            if (btn) {
                const row = btn.closest('.grid');
                row.classList.toggle('opacity-40');
            }
        });
    }
});
