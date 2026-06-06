package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.auth.JwtResponse;
import com.cineclubsalamanca.dto.auth.LoginRequest;
import com.cineclubsalamanca.dto.auth.RegisterRequest;
import com.cineclubsalamanca.entity.Usuario;
import com.cineclubsalamanca.repository.UsuarioRepository;
import com.cineclubsalamanca.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public JwtResponse register(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(req.nombre())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();

        usuario = usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new JwtResponse(token, usuario.getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getRol().name());
    }

    public JwtResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(req.email()).orElseThrow();
        UserDetails userDetails = userDetailsService.loadUserByUsername(req.email());
        String token = jwtUtil.generateToken(userDetails);

        return new JwtResponse(token, usuario.getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getRol().name());
    }
}
