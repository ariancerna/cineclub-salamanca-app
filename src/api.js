/**
 * SERVICIO DE API (Capa de Abstracción)
 * 
 * Para conectar con Spring Boot:
 * Simplemente reemplaza el contenido de estas funciones con:
 * return fetch('/api/peliculas').then(res => res.json());
 */

export const ApiService = {
    // Obtener todas las películas
    async getMovies() {
        // Simulación de datos que vendrían de tu DB
        return [
            { 
                id: 1, 
                titulo: "Metropolis", 
                director: "Fritz Lang", 
                duracion: "153 min",
                sinopsis: "Una obra maestra del expresionismo alemán que presenta una visión distópica de una ciudad del futuro dividida entre obreros y pensadores.",
                img: "https://lh3.googleusercontent.com/aida-public/AB6AXuDH-lDuWnXEPgVdQ_t87SvmMaZRKfXlPxFvG4MHvNtYUiGr9RT5JZ-sUnyUgJhKJxsc17nL5cWgXeJwgEtN7mdoK3B63AzRQD0nFB9ITohTkvFFV8fN_Fh8Vd3mPpoxIzGx7FJc-JtwpcnAAfnctn0WmRBuvK7fK_gKff7ZLV_lCDegtkJARcGfL5dPKf3TygEpGOjk-DY7iPzMIAblvjk6RT-ikrs_U6BlYk8K_SszkDHH39vOawn5Y5YVNvJfIdBHBeJjWN7G9wji" 
            },
            { 
                id: 2, 
                titulo: "Ciudadano Kane", 
                director: "Orson Welles", 
                duracion: "119 min",
                sinopsis: "Tras la muerte de un magnate de la prensa, un grupo de periodistas trata de descubrir el significado de su última palabra: 'Rosebud'.",
                img: "https://lh3.googleusercontent.com/aida-public/AB6AXuAokG0zefQ-bUOT4TU3lj9zM37Ix8ZL7_gKoT8I47_5tG7lDOev2YlpTjrJjhx_TSn9mDQYXXU5fgY3g2Qsx5a9aa7eUUuaWJIWhIzVGcnc99hIbFT7fB1f5ypfBQWmQS3RICk_1mkSZ2bj50Qh7ptIC-dKANAKJftRRIL7LyZ0Iu1SK8vrki7US2exfvM3338AGhVC4jSr2HDFYGTMUk6VgValqtdDs4HewFU4uYh_jGv7MYZaGQCWMJGkuXYXIoLGYZwTxbcnHBBQ" 
            },
            { 
                id: 3, 
                titulo: "El Séptimo Sello", 
                director: "Ingmar Bergman", 
                duracion: "96 min",
                sinopsis: "Un caballero de regreso de las cruzadas se encuentra con la Muerte y la desafía a una partida de ajedrez para ganar tiempo.",
                img: "https://lh3.googleusercontent.com/aida-public/AB6AXuCYZknd_fqv0RKgAAWnaWZznmEqptuul_UHWSvXrokghz5a-dS1hZOKx_oNS2JGYJcvkPam4rVbXuMgnv0VQlDc6Xy8xuvwyMhuka4nhQELK_kljPwQdIh7FerjZjowemAo7Z5OlqQfLHynLtCRuldmjSBfq21qhssT2ySt8tgRT0yv2pVyI2TJqeoTzyLhP7tCyZ-ygJALrQFwINnHbp-iPsvwAkQtWi_u55LpH02oNe6o3i40o5rtsY505UYOV2H4JIn0IRHAVgxP" 
            }
        ];
    },

    // Obtener una película por ID
    async getMovieById(id) {
        const movies = await this.getMovies();
        return movies.find(m => m.id == id);
    },

    // Obtener asientos ocupados para una película específica
    async getOccupiedSeats(movieId) {
        // En Spring Boot: return fetch(`/api/peliculas/${movieId}/asientos-ocupados`).then(res => res.json());
        // Por ahora simulamos algunos asientos ocupados
        const mockData = {
            "1": ['A-1', 'A-2', 'B-4'],
            "2": ['C-1', 'C-2', 'D-5', 'D-6'],
            "3": ['B-7', 'B-8', 'A-4']
        };
        return mockData[movieId] || [];
    },

    // Guardar una reserva
    async saveBooking(bookingData) {
        // En Spring Boot usarías un POST aquí:
        /*
        return fetch('/api/reservas', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(bookingData)
        }).then(res => res.json());
        */
        const reservas = JSON.parse(localStorage.getItem('reservas') || '[]');
        reservas.push(bookingData);
        localStorage.setItem('reservas', JSON.stringify(reservas));
        return { success: true, booking: bookingData };
    },

    // Obtener mis reservaciones
    async getMyBookings() {
        // En Spring Boot: return fetch('/api/mis-reservas').then(res => res.json());
        return JSON.parse(localStorage.getItem('reservas') || '[]');
    },

    // Obtener todas las reservaciones (Admin)
    async getAllBookings() {
        return JSON.parse(localStorage.getItem('reservas') || '[]');
    },

    // Obtener snacks disponibles
    async getSnacks() {
        return [
            { id: 1, nombre: "Combo Clásico", precio: 35.0, descripcion: "Popcorn grande + Bebida", img: "https://images.unsplash.com/photo-1572177191856-3cde618dee1f?auto=format&fit=crop&q=80&w=150" },
            { id: 2, nombre: "Nachos con Queso", precio: 22.0, descripcion: "Con salsa cheddar caliente", img: "https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?auto=format&fit=crop&q=80&w=150" },
            { id: 3, nombre: "Popcorn Extra", precio: 18.0, descripcion: "Porción familiar salada", img: "https://images.unsplash.com/photo-1585647347483-22b66260dfff?auto=format&fit=crop&q=80&w=150" },
            { id: 4, nombre: "Chocolate Premium", precio: 12.0, descripcion: "Barra de cacao 70%", img: "https://images.unsplash.com/photo-1511381939415-e44015466834?auto=format&fit=crop&q=80&w=150" },
            { id: 5, nombre: "Gaseosa Grande", precio: 10.0, descripcion: "Sabor a elección 500ml", img: "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&q=80&w=150" },
            { id: 6, nombre: "Agua Mineral", precio: 6.0, descripcion: "Sin gas 600ml", img: "https://images.unsplash.com/photo-1560023907-5f339617ea30?auto=format&fit=crop&q=80&w=150" }
        ];
    },
};
