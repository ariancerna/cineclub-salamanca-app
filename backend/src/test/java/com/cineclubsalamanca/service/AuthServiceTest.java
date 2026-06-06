package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.auth.JwtResponse;
import com.cineclubsalamanca.dto.auth.LoginRequest;
import com.cineclubsalamanca.dto.auth.RegisterRequest;
import com.cineclubsalamanca.entity.Rol;
import com.cineclubsalamanca.entity.Usuario;
import com.cineclubsalamanca.repository.UsuarioRepository;
import com.cineclubsalamanca.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Registro exitoso devuelve token JWT con los datos del usuario")
    void register_debeRetornarToken_cuandoEmailNuevo() {
        RegisterRequest req = new RegisterRequest("Arian Cerna", "arian@test.com", "password123");

        Usuario usuarioGuardado = Usuario.builder()
                .id(1L)
                .nombre("Arian Cerna")
                .email("arian@test.com")
                .passwordHash("hashed_password")
                .rol(Rol.ROLE_USER)
                .build();

        UserDetails userDetails = mock(UserDetails.class);

        when(usuarioRepository.existsByEmail("arian@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(userDetailsService.loadUserByUsername("arian@test.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt.token.generado");

        JwtResponse response = authService.register(req);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.generado");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo("arian@test.com");
        assertThat(response.nombre()).isEqualTo("Arian Cerna");
        assertThat(response.rol()).isEqualTo("ROLE_USER");
        verify(passwordEncoder).encode("password123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("No se puede registrar el mismo email dos veces")
    void register_debeLanzarExcepcion_cuandoEmailDuplicado() {
        RegisterRequest req = new RegisterRequest("Otro Usuario", "arian@test.com", "password123");
        when(usuarioRepository.existsByEmail("arian@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("La contraseña se encripta con BCrypt antes de guardarse en la BD")
    void register_debeEncriptarPassword_antesDeGuardar() {
        RegisterRequest req = new RegisterRequest("Fabian Morocho", "fabian@test.com", "miPassword99");

        Usuario guardado = Usuario.builder()
                .id(2L).nombre("Fabian Morocho").email("fabian@test.com")
                .passwordHash("bcrypt_hash").rol(Rol.ROLE_USER).build();

        when(usuarioRepository.existsByEmail("fabian@test.com")).thenReturn(false);
        when(passwordEncoder.encode("miPassword99")).thenReturn("bcrypt_hash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);
        when(userDetailsService.loadUserByUsername("fabian@test.com")).thenReturn(mock(UserDetails.class));
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.register(req);

        verify(passwordEncoder).encode("miPassword99");
    }

    @Test
    @DisplayName("Login con credenciales válidas devuelve token JWT")
    void login_debeRetornarToken_cuandoCredencialesValidas() {
        LoginRequest req = new LoginRequest("joan@test.com", "miPassword123");

        Usuario usuario = Usuario.builder()
                .id(2L)
                .nombre("Joan Pelayo")
                .email("joan@test.com")
                .passwordHash("hashed")
                .rol(Rol.ROLE_USER)
                .build();

        UserDetails userDetails = mock(UserDetails.class);

        when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
        when(userDetailsService.loadUserByUsername("joan@test.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt.login.token");

        JwtResponse response = authService.login(req);

        assertThat(response.token()).isEqualTo("jwt.login.token");
        assertThat(response.nombre()).isEqualTo("Joan Pelayo");
        assertThat(response.email()).isEqualTo("joan@test.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Las credenciales pasan por Spring Security antes de generar el token")
    void login_debeInvocarAuthManager() {
        LoginRequest req = new LoginRequest("fabrizio@test.com", "pass1234");

        Usuario usuario = Usuario.builder()
                .id(3L).nombre("Fabrizio Santillan").email("fabrizio@test.com")
                .passwordHash("hashed").rol(Rol.ROLE_USER).build();

        when(usuarioRepository.findByEmail("fabrizio@test.com")).thenReturn(Optional.of(usuario));
        when(userDetailsService.loadUserByUsername("fabrizio@test.com")).thenReturn(mock(UserDetails.class));
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(req);

        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken
                        && auth.getPrincipal().equals("fabrizio@test.com"))
        );
    }
}
