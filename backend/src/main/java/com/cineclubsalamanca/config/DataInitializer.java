package com.cineclubsalamanca.config;

import com.cineclubsalamanca.entity.*;
import com.cineclubsalamanca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

        @Value("${data.funciones.start-days:3}")
        private int funcionesStartDays;

    @Override
    public void run(String... args) {
        if (funcionRepository.count() > 0) return;

        usuarioRepository.save(Usuario.builder()
                .nombre("Admin")
                .email("admin@cineclub.com")
                .passwordHash(passwordEncoder.encode("admin1234"))
                .rol(Rol.ROLE_ADMIN)
                .build());

        Pelicula p1 = peliculaRepository.save(Pelicula.builder()
                .titulo("Metropolis")
                .director("Fritz Lang")
                .duracionMinutos(153)
                .sinopsis("Una obra maestra del expresionismo alemán que presenta una visión distópica de una ciudad del futuro dividida entre obreros y pensadores.")
                .aficheUrl("https://commons.wikimedia.org/wiki/Special:FilePath/Boris%20Bilinski%20-%20Filmplakat%20f%C3%BCr%20Metropolis%20restored.jpg?width=600")
                .build());

        Pelicula p2 = peliculaRepository.save(Pelicula.builder()
                .titulo("Ciudadano Kane")
                .director("Orson Welles")
                .duracionMinutos(119)
                .sinopsis("Tras la muerte de un magnate de la prensa, un grupo de periodistas trata de descubrir el significado de su última palabra: 'Rosebud'.")
                .aficheUrl("https://commons.wikimedia.org/wiki/Special:FilePath/Citizen%20Kane%20poster,%201941%20(Style%20A).jpg?width=600")
                .build());

        Pelicula p3 = peliculaRepository.save(Pelicula.builder()
                .titulo("El Séptimo Sello")
                .director("Ingmar Bergman")
                .duracionMinutos(96)
                .sinopsis("Un caballero de regreso de las cruzadas se encuentra con la Muerte y la desafía a una partida de ajedrez para ganar tiempo.")
                .aficheUrl("https://upload.wikimedia.org/wikipedia/en/thumb/6/69/Seventhsealposter.jpg/250px-Seventhsealposter.jpg")
                .build());

        Pelicula p4 = peliculaRepository.save(Pelicula.builder()
                .titulo("La Dolce Vita")
                .director("Federico Fellini")
                .duracionMinutos(174)
                .sinopsis("Un periodista recorre la vida nocturna de Roma mientras busca sentido entre celebridades, fiestas y encuentros fugaces.")
                .aficheUrl("https://upload.wikimedia.org/wikipedia/en/c/c1/La_Dolce_Vita_%281960_film%29_coverart.jpg")
                .build());

        Pelicula p5 = peliculaRepository.save(Pelicula.builder()
                .titulo("Rashomon")
                .director("Akira Kurosawa")
                .duracionMinutos(88)
                .sinopsis("Cuatro versiones contradictorias de un crimen revelan la fragilidad de la verdad y la memoria humana.")
                .aficheUrl("https://commons.wikimedia.org/wiki/Special:FilePath/Rashomon%20(US%20poster).jpg?width=600")
                .build());

        Pelicula p6 = peliculaRepository.save(Pelicula.builder()
                .titulo("Los 400 Golpes")
                .director("Francois Truffaut")
                .duracionMinutos(99)
                .sinopsis("Antoine Doinel, un adolescente parisino, enfrenta la incomprension adulta y descubre la libertad en medio de la rebeldia.")
                .aficheUrl("https://upload.wikimedia.org/wikipedia/en/f/f9/Quatre_coups2.jpg")
                .build());

        Pelicula p7 = peliculaRepository.save(Pelicula.builder()
                .titulo("In the Mood for Love")
                .director("Wong Kar-wai")
                .duracionMinutos(98)
                .sinopsis("Dos vecinos en Hong Kong descubren una traicion compartida y construyen una intimidad marcada por el silencio.")
                .aficheUrl("https://upload.wikimedia.org/wikipedia/en/thumb/4/45/In_the_Mood_for_Love_movie.jpg/250px-In_the_Mood_for_Love_movie.jpg")
                .build());

        Pelicula p8 = peliculaRepository.save(Pelicula.builder()
                .titulo("Cinema Paradiso")
                .director("Giuseppe Tornatore")
                .duracionMinutos(124)
                .sinopsis("Un cineasta recuerda su infancia en Sicilia y la amistad que formo con el proyeccionista de su pueblo.")
                .aficheUrl("https://upload.wikimedia.org/wikipedia/en/thumb/8/86/CinemaParadiso.jpg/250px-CinemaParadiso.jpg")
                .build());

        LocalDateTime base = LocalDateTime.now().plusDays(funcionesStartDays).withHour(19).withMinute(30).withSecond(0).withNano(0);

        funcionRepository.save(Funcion.builder().pelicula(p1).fechaHora(base).sala("Sala Principal").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p2).fechaHora(base.plusDays(1).withHour(20)).sala("Sala Principal").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p3).fechaHora(base.plusDays(2).withHour(18)).sala("Sala Principal").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p4).fechaHora(base.plusDays(3).withHour(19)).sala("Sala Lumiere").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p5).fechaHora(base.plusDays(4).withHour(21)).sala("Sala Kurosawa").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p6).fechaHora(base.plusDays(5).withHour(18)).sala("Sala Nouvelle").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p7).fechaHora(base.plusDays(6).withHour(20)).sala("Sala Oriente").aforoMaximo(32).aforoDisponible(32).build());
        funcionRepository.save(Funcion.builder().pelicula(p8).fechaHora(base.plusDays(7).withHour(17)).sala("Sala Principal").aforoMaximo(32).aforoDisponible(32).build());

        Object[][] prods = {
            {"Combo Clásico",    "35.00", "Popcorn grande + Bebida",      "https://images.unsplash.com/photo-1572177191856-3cde618dee1f?auto=format&fit=crop&q=80&w=150"},
            {"Nachos con Queso", "22.00", "Con salsa cheddar caliente",   "https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?auto=format&fit=crop&q=80&w=150"},
            {"Popcorn Extra",    "18.00", "Porción familiar salada",      "https://images.unsplash.com/photo-1585647347483-22b66260dfff?auto=format&fit=crop&q=80&w=150"},
            {"Chocolate Premium","12.00", "Barra de cacao 70%",           "https://images.unsplash.com/photo-1511381939415-e44015466834?auto=format&fit=crop&q=80&w=150"},
            {"Gaseosa Grande",   "10.00", "Sabor a elección 500ml",       "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&q=80&w=150"},
            {"Agua Mineral",      "6.00", "Sin gas 600ml",                "https://images.unsplash.com/photo-1560023907-5f339617ea30?auto=format&fit=crop&q=80&w=150"}
        };
        for (Object[] r : prods) {
            productoRepository.save(Producto.builder()
                    .nombre((String) r[0])
                    .precio(new BigDecimal((String) r[1]))
                    .descripcion((String) r[2])
                    .imagenUrl((String) r[3])
                    .build());
        }
    }
}
