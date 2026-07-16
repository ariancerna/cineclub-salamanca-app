package com.cineclubsalamanca.integration;

import com.cineclubsalamanca.dto.auth.LoginRequest;
import com.cineclubsalamanca.dto.auth.RegisterRequest;
import com.cineclubsalamanca.entity.Rol;
import com.cineclubsalamanca.entity.Usuario;
import com.cineclubsalamanca.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de seguridad de extremo a extremo sobre la API REST.
 *
 * <p>Levantan el contexto completo de Spring (filtros de seguridad incluidos) contra una
 * base H2 en memoria. A diferencia de las pruebas unitarias de servicios, aquí se verifica
 * la cadena de filtros real: autenticación JWT, autorización por rol y validación de entrada.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de seguridad de la API")
class SeguridadIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String EMAIL_USER = "espectador@test.com";
    private static final String EMAIL_ADMIN = "admin@test.com";
    private static final String PASSWORD = "clave_segura_123";

    @BeforeEach
    void prepararUsuarios() {
        usuarioRepository.deleteAll();

        usuarioRepository.save(Usuario.builder()
                .nombre("Espectador")
                .email(EMAIL_USER)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .rol(Rol.ROLE_USER)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador")
                .email(EMAIL_ADMIN)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .rol(Rol.ROLE_ADMIN)
                .build());
    }

    /** Autentica y devuelve el token JWT emitido. */
    private String obtenerToken(String email) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(cuerpo).get("token").asText();
    }

    // ---------- Control de acceso ----------

    @Test
    @DisplayName("Un endpoint protegido rechaza la petición sin token (401)")
    void endpointProtegido_debeRechazarSinToken() throws Exception {
        mockMvc.perform(get("/api/reservas/mis-reservas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token con firma inválida es rechazado (401)")
    void endpointProtegido_debeRechazarTokenConFirmaInvalida() throws Exception {
        // Token bien formado pero firmado con otra clave
        String tokenFalso = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSJ9.firma_falsificada";

        mockMvc.perform(get("/api/reservas/mis-reservas")
                        .header("Authorization", "Bearer " + tokenFalso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un usuario autenticado accede a sus propias reservas (200)")
    void misReservas_debePermitirUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/api/reservas/mis-reservas")
                        .header("Authorization", "Bearer " + obtenerToken(EMAIL_USER)))
                .andExpect(status().isOk());
    }

    // ---------- Autorización por rol ----------

    @Test
    @DisplayName("Un usuario sin rol admin no puede listar las reservas de una función (403)")
    void endpointAdmin_debeRechazarUsuarioNoAdmin() throws Exception {
        mockMvc.perform(get("/api/reservas/funcion/1")
                        .header("Authorization", "Bearer " + obtenerToken(EMAIL_USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un administrador sí puede listar las reservas de una función (200)")
    void endpointAdmin_debePermitirAdmin() throws Exception {
        mockMvc.perform(get("/api/reservas/funcion/1")
                        .header("Authorization", "Bearer " + obtenerToken(EMAIL_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un usuario sin rol admin no puede crear películas (403)")
    void crearPelicula_debeRechazarUsuarioNoAdmin() throws Exception {
        String pelicula = """
                {"titulo":"Intrusa","sinopsis":"x","director":"y","duracionMinutos":90,"aficheUrl":null}
                """;

        mockMvc.perform(post("/api/peliculas")
                        .header("Authorization", "Bearer " + obtenerToken(EMAIL_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pelicula))
                .andExpect(status().isForbidden());
    }

    // ---------- Endpoints públicos ----------

    @Test
    @DisplayName("La cartelera es pública: se consulta sin autenticación (200)")
    void cartelera_debeSerPublica() throws Exception {
        mockMvc.perform(get("/api/peliculas"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La sonda de salud es pública para el balanceador (200)")
    void health_debeSerPublico() throws Exception {
        // Sin funciones cargadas la cartelera reporta SIN_CARTELERA, que no degrada la salud global
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("La sonda de salud no expone detalles a un usuario no autorizado")
    void health_noDebeExponerDetallesSinAutorizacion() throws Exception {
        // show-details=when-authorized: el anónimo ve el estado, no los componentes internos
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("Las métricas no son públicas: requieren rol admin (401 sin token)")
    void metricas_noDebenSerPublicas() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Credenciales y contraseñas ----------

    @Test
    @DisplayName("El login con contraseña incorrecta no emite token (401)")
    void login_debeRechazarPasswordIncorrecta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(EMAIL_USER, "password_equivocada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La contraseña se almacena cifrada con BCrypt, nunca en texto plano")
    void registro_debeAlmacenarPasswordCifrada() throws Exception {
        RegisterRequest req = new RegisterRequest("Nuevo Socio", "nuevo@test.com", PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Usuario guardado = usuarioRepository.findByEmail("nuevo@test.com").orElseThrow();

        assertThat(guardado.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(guardado.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, guardado.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("El registro rechaza contraseñas de menos de 8 caracteres (400)")
    void registro_debeRechazarPasswordCorta() throws Exception {
        RegisterRequest req = new RegisterRequest("Socio", "corta@test.com", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(usuarioRepository.findByEmail("corta@test.com")).isEmpty();
    }

    @Test
    @DisplayName("El registro rechaza un email con formato inválido (400)")
    void registro_debeRechazarEmailInvalido() throws Exception {
        RegisterRequest req = new RegisterRequest("Socio", "esto-no-es-un-email", PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La respuesta de autenticación nunca expone el hash de la contraseña")
    void login_noDebeExponerPasswordHash() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL_USER, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("passwordHash");
        assertThat(cuerpo).doesNotContain("$2a$");
        assertThat(cuerpo).doesNotContain(PASSWORD);
    }

    // ---------- Inyección SQL ----------

    @Test
    @DisplayName("Una carga de inyección SQL en el login no altera la base de datos")
    void login_debeResistirInyeccionSql() throws Exception {
        long usuariosAntes = usuarioRepository.count();
        String payload = "' OR '1'='1'; DROP TABLE usuario; --";

        // JPA parametriza las consultas, por lo que la carga se trata como un email literal
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(payload, payload))))
                .andExpect(status().is4xxClientError());

        assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
    }
}
