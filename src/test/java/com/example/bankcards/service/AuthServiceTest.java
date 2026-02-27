package com.example.bankcards.service;

import com.example.bankcards.dto.AuthRequest;
import com.example.bankcards.dto.AuthResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.EmailAlreadyExistsException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private AuthRequest request;
    private User savedUser;

    @BeforeEach
    void setUp() {
        request = new AuthRequest();
        request.setEmail("user@bank.com");
        request.setPassword("password123");

        savedUser = User.builder()
                .id(1L)
                .email("user@bank.com")
                .password("encodedPassword")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Успешная регистрация — возвращает токен и данные пользователя")
        void register_success() {
            when(userRepository.existsByEmail("user@bank.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Authentication auth = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

            AuthResponse response = authService.register(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("user@bank.com");
            assertThat(response.getRole()).isEqualTo("ROLE_USER");

            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Регистрация с уже занятым email — ошибка 409")
        void register_emailAlreadyExists_throwsException() {
            when(userRepository.existsByEmail("user@bank.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("user@bank.com");

            verify(userRepository, never()).save(any());
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("При регистрации пароль хешируется, а не сохраняется в открытом виде")
        void register_passwordIsEncoded() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
            when(tokenProvider.generateToken(any())).thenReturn("token");

            authService.register(request);

            // Проверяем что encode был вызван с исходным паролем
            verify(passwordEncoder).encode("password123");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Успешный вход — возвращает токен без лишнего DB-запроса")
        void login_success() {
            org.springframework.security.core.userdetails.User principal =
                    new org.springframework.security.core.userdetails.User(
                            "user@bank.com",
                            "encodedPassword",
                            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                    );

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(principal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

            AuthResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("user@bank.com");
            assertThat(response.getRole()).isEqualTo("ROLE_USER");

            // Убеждаемся что лишний DB-запрос больше не делается
            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("Вход с неверным паролем — выбрасывает BadCredentialsException")
        void login_wrongPassword_throwsException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByEmail(any());
            verify(tokenProvider, never()).generateToken(any());
        }
    }
}
