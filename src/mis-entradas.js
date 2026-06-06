/**
 * Lógica para la página de Mis Entradas
 */

import { ApiService } from './api.js';

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const container = document.getElementById('tickets-container');
    const reservas = await ApiService.getMyBookings();

    if (reservas.length === 0) {
        container.innerHTML = `
            <div class="p-20 text-center border-2 border-dashed border-brand-outline">
               <p class="text-brand-outline font-bold italic">Aún no has realizado ninguna reserva.</p>
               <a href="index.html" class="mt-6 btn-primary inline-block shadow-hard">Ver Películas</a>
            </div>`;
    } else {
        container.innerHTML = reservas.map((t, idx) => `
            <div class="border border-brand-ink p-6 md:p-10 bg-white grid grid-cols-1 md:grid-cols-12 gap-8 md:gap-12 items-center hover:translate-x-1 transition-transform cursor-pointer" id="ticket-${idx}">
                <div class="md:col-span-2 aspect-[3/4] bg-gray-100 overflow-hidden border border-brand-outline w-1/3 md:w-full mx-auto md:mx-0">
                    <img src="${t.img || 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&q=80&w=300'}" class="w-full h-full object-cover grayscale" alt="Poster">
                </div>
                <div class="md:col-span-10 text-center md:text-left">
                    <span class="inline-block px-3 py-1 bg-brand-surface border border-brand-outline text-brand-primary text-[10px] font-bold uppercase mb-6 md:mb-6 tracking-widest leading-none">Reserva #${t.id}</span>
                    <h3 class="text-3xl md:text-4xl font-bold uppercase mb-2 leading-tight">${t.peli}</h3>
                    <p class="text-brand-outline font-bold text-sm">Espectador: ${t.user}</p>
                    
                    <div class="grid grid-cols-2 lg:grid-cols-4 gap-6 md:gap-8 mt-8 md:mt-12 bg-brand-surface p-6 border border-dashed border-brand-outline text-left">
                        <div><span class="block text-[8px] font-bold text-brand-outline uppercase">Fecha Compra</span><span class="text-xs md:text-sm font-bold">${t.fecha.split(',')[0]}</span></div>
                        <div><span class="block text-[8px] font-bold text-brand-outline uppercase">Sala</span><span class="text-xs md:text-sm font-bold">Sala 01</span></div>
                        <div><span class="block text-[8px] font-bold text-brand-outline uppercase">Butaca</span><span class="text-xs md:text-sm font-bold uppercase">${t.seat}</span></div>
                        <div class="flex items-center gap-2 text-brand-primary font-bold col-span-2 lg:col-span-1 border-t lg:border-none pt-4 lg:pt-0"><i data-lucide="ticket" class="w-5 h-5"></i><span class="text-[10px] uppercase">Ver Digital</span></div>
                    </div>
                </div>
            </div>
        `).join('');

        // Listeners
        reservas.forEach((t, idx) => {
            const el = document.getElementById(`ticket-${idx}`);
            if (el) {
                el.onclick = () => {
                    localStorage.setItem('ultima_reserva', JSON.stringify(reservas[idx]));
                    window.location.href = 'detalle-ticket.html';
                };
            }
        });
    }
    
    if (window.lucide) lucide.createIcons();
});
