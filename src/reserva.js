/**
 * Lógica para la página de Reserva (Asientos y Modal de Snacks)
 */

import { ApiService } from './api.js';

document.addEventListener('DOMContentLoaded', async () => {
    if (window.lucide) lucide.createIcons();

    const grid = document.getElementById('seat-grid');
    const resumen = document.getElementById('resumen-asiento');
    const btnConfirmarAsiento = document.getElementById('btn-confirmar');
    const inputNombre = document.getElementById('input-nombre');
    const modal = document.getElementById('modal-snacks');
    const snacksContainer = document.getElementById('snacks-list');

    let selectedSeat = null;
    const movieId = new URLSearchParams(window.location.search).get('id');
    const pelicula = await ApiService.getMovieById(movieId) || { titulo: "Película" };
    const PRODUCTOS = await ApiService.getSnacks();
    let occupiedSeats = await ApiService.getOccupiedSeats(movieId);
    
    let seleccionSnacks = {}; // { id: cantidad }

    function renderSeats() {
        const rows = ['A','B','C','D'];
        grid.innerHTML = rows.map(r => `
            <div class="flex gap-3 min-w-max">
                <span class="w-6 font-bold text-brand-outline uppercase text-[10px] flex items-center justify-end">${r}</span>
                <div class="flex gap-2">
                    ${[1,2,3,4,5,6,7,8].map(n => {
                        const id = `${r}-${n}`;
                        const isOcc = occupiedSeats.includes(id);
                        const isSel = selectedSeat === id;
                        return `
                            <button data-seat="${id}" 
                                class="w-8 h-8 md:w-10 md:h-10 border text-[10px] md:text-xs flex items-center justify-center transition-all ${isOcc ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : (isSel ? 'bg-brand-primary text-white border-brand-primary shadow-hard scale-110' : 'bg-white border-brand-ink hover:border-brand-primary')}"
                                ${isOcc ? 'disabled' : ''}>
                                ${isOcc ? '×' : id}
                            </button>
                        `;
                    }).join('')}
                </div>
            </div>
        `).join('');

        grid.querySelectorAll('button[data-seat]').forEach(button => {
            button.onclick = () => {
                const id = button.getAttribute('data-seat');
                selectedSeat = (selectedSeat === id) ? null : id;
                updateUI();
                renderSeats();
            };
        });
    }

    function renderSnacks() {
        snacksContainer.innerHTML = PRODUCTOS.map(p => `
            <div class="flex flex-col sm:flex-row gap-4 sm:gap-6 p-4 border border-brand-ink bg-white">
                <div class="w-full sm:w-20 h-32 sm:h-20 bg-gray-100 overflow-hidden">
                    <img src="${p.img}" class="w-full h-full object-cover grayscale">
                </div>
                <div class="flex-grow">
                    <div class="flex justify-between items-start">
                        <h4 class="font-bold uppercase text-sm">${p.nombre}</h4>
                        <span class="font-bold text-sm">S/ ${p.precio.toFixed(2)}</span>
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

        const total = PRODUCTOS.reduce((acc, p) => acc + (p.precio * (seleccionSnacks[p.id] || 0)), 0);
        const totalDisplay = document.getElementById('snacks-total');
        if (totalDisplay) totalDisplay.innerText = `S/ ${total.toFixed(2)}`;

        if (window.lucide) lucide.createIcons();
    }

    window.updateSnack = (id, delta) => {
        const current = seleccionSnacks[id] || 0;
        seleccionSnacks[id] = Math.max(0, current + delta);
        renderSnacks();
    };

    function updateUI() {
        if (selectedSeat) {
            const [fila, num] = selectedSeat.split('-');
            resumen.innerHTML = `Fila ${fila}, Asiento ${num}`;
            resumen.classList.remove('text-brand-outline', 'italic');
        } else {
            resumen.innerHTML = 'Ninguno seleccionado';
            resumen.classList.add('text-brand-outline', 'italic');
        }
        btnConfirmarAsiento.disabled = !selectedSeat || !inputNombre.value.trim();
    }

    inputNombre.addEventListener('input', updateUI);
    btnConfirmarAsiento.onclick = () => {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    };

    window.finalizarReserva = async () => {
        const confirmados = PRODUCTOS
            .filter(p => seleccionSnacks[p.id] > 0)
            .map(p => ({ ...p, cantidad: seleccionSnacks[p.id] }));

        const reserva = {
            id: Math.floor(Math.random() * 90000) + 10000,
            peli: pelicula.titulo,
            img: pelicula.img,
            movieId: movieId,
            user: inputNombre.value,
            seat: selectedSeat,
            snacks: confirmados,
            fecha: new Date().toLocaleString()
        };

        await ApiService.saveBooking(reserva);
        localStorage.setItem('ultima_reserva', JSON.stringify(reserva));
        window.location.href = 'detalle-ticket.html';
    };

    renderSeats();
    renderSnacks();
});
