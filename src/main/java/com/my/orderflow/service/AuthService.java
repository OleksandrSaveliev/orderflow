package com.my.orderflow.service;

import com.my.orderflow.dto.auth.AuthResponseDto;
import com.my.orderflow.dto.auth.LoginRequestDto;
import com.my.orderflow.dto.auth.RefreshResponseDto;
import com.my.orderflow.dto.auth.RegisterRequestDto;
import com.my.orderflow.exception.EmailAlreadyExistsException;
import com.my.orderflow.exception.InvalidTokenException;
import com.my.orderflow.exception.UserNotFoundException;
import com.my.orderflow.model.RefreshToken;
import com.my.orderflow.model.User;
import com.my.orderflow.model.enums.Role;
import com.my.orderflow.repository.RefreshTokenRepository;
import com.my.orderflow.repository.UserRepository;
import com.my.orderflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
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
        saveRefreshToken(savedUser, refreshToken);

        return new AuthResponseDto(accessToken, refreshToken, TOKEN_TYPE, accessTokenExpiration);
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        if (user == null) {
            throw new IllegalStateException("Authentication principal is null");
        }

        refreshTokenRepository.revokeAllUserTokens(user.getId());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        saveRefreshToken(user, refreshToken);

        log.info("User logged in: {}", user.getEmail());

        return new AuthResponseDto(accessToken, refreshToken, TOKEN_TYPE, accessTokenExpiration);
    }

    @Transactional
    public RefreshResponseDto refreshAccessToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or revoked"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(storedToken.getUserId()));

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        log.debug("Refreshed access token for user: {}", user.getEmail());

        return new RefreshResponseDto(newAccessToken, TOKEN_TYPE, accessTokenExpiration);
    }

    @Transactional
    public void logout(String refreshToken) {

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Token not found or already revoked"));

        refreshTokenRepository.revokeAllUserTokens(storedToken.getUserId());
        log.info("User logged out, tokens revoked for userId: {}", storedToken.getUserId());
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}