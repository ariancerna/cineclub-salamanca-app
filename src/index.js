/**
 * Lógica para la Cartelera (index.html)
 */

import { ApiService } from './api.js';

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const grid = document.getElementById('cartelera-grid');
    const movies = await ApiService.getMovies();

    // Renderizar grilla
    if (grid) {
        grid.innerHTML = movies.map(p => `
            <div class="group border border-brand-outline bg-white overflow-hidden hover:-translate-y-2 transition-all flex flex-col h-full shadow-hard-active">
                <div class="aspect-[4/3] bg-gray-100 overflow-hidden grayscale group-hover:grayscale-0 transition-all duration-500 relative">
                    <img src="${p.img}" class="w-full h-full object-cover" alt="${p.titulo}">
                    <div class="absolute top-4 left-4 bg-brand-ink text-white text-[8px] font-bold uppercase py-1 px-3 tracking-widest z-10">
                        ${p.duracion}
                    </div>
                </div>
                <div class="p-6 md:p-8 flex flex-col flex-grow">
                    <h3 class="text-2xl font-bold mb-1 uppercase tracking-tight">${p.titulo}</h3>
                    <p class="text-brand-outline text-[10px] font-bold uppercase mb-4 tracking-widest">Dir. ${p.director}</p>
                    <p class="text-xs md:text-sm text-gray-600 line-clamp-3 mb-8 flex-grow leading-relaxed">
                        ${p.sinopsis}
                    </p>
                    <a href="reserva.html?id=${p.id}" class="w-full inline-block text-center btn-outline border-brand-outline hover:border-brand-primary text-xs py-3">
                        Comprar Entradas
                    </a>
                </div>
            </div>
        `).join('');
    }

    if (window.lucide) lucide.createIcons();
});
