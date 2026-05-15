package com.my.orderflow.service;

import com.my.orderflow.dto.auth.AuthResponseDto;
import com.my.orderflow.dto.auth.LoginRequestDto;
import com.my.orderflow.dto.auth.RefreshTokenResponseDto;
import com.my.orderflow.dto.auth.RegisterRequestDto;
import com.my.orderflow.model.RefreshToken;
import com.my.orderflow.model.User;
import com.my.orderflow.model.enums.Role;
import com.my.orderflow.repository.RefreshTokenRepository;
import com.my.orderflow.repository.UserRepository;
import com.my.orderflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}", savedUser.getId());

        String accessToken = jwtService.generateAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId());

        saveRefreshToken(savedUser.getId(), refreshToken);

        return new AuthResponseDto(accessToken, refreshToken, "Bearer", 900000L);
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        refreshTokenRepository.revokeAllUserTokens(user.getId());
        saveRefreshToken(user.getId(), refreshToken);

        log.info("User logged in: {}", user.getEmail());

        return new AuthResponseDto(accessToken, refreshToken, "Bearer", 900000L);
    }

    @Transactional
    public RefreshTokenResponseDto refreshAccessToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);


        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found or revoked"));


        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        log.debug("Refreshed access token for user: {}", user.getEmail());

        return new RefreshTokenResponseDto(newAccessToken, "Bearer", 900000L);
    }

    @Transactional
    public void logout(String accessToken) {
        if (jwtService.isTokenValid(accessToken)) {
            UUID userId = jwtService.extractUserId(accessToken);
            refreshTokenRepository.revokeAllUserTokens(userId);
            log.info("User logged out, tokens revoked for userId: {}", userId);
        }
    }

    private void saveRefreshToken(UUID userId, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}