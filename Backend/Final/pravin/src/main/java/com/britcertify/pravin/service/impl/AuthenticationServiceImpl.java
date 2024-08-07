package com.britcertify.pravin.service.impl;

import static com.britcertify.pravin.enumerated.TokenType.BEARER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.britcertify.pravin.dto.request.LoginRequest;
import com.britcertify.pravin.dto.request.RegisterRequest;
import com.britcertify.pravin.dto.response.LoginResponse;
import com.britcertify.pravin.dto.response.RegisterResponse;
import com.britcertify.pravin.dto.response.UserDetailsResponse;
import com.britcertify.pravin.enumerated.Role;
import com.britcertify.pravin.model.Token;
import com.britcertify.pravin.model.User;
import com.britcertify.pravin.repository.TokenRepository;
import com.britcertify.pravin.repository.UserRepository;
import com.britcertify.pravin.service.AuthenticationService;
import com.britcertify.pravin.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthenticationServiceImpl implements AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .mobilenumber(request.getMobilenumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole().toUpperCase()))
                .build();
        userRepository.save(user);
        return RegisterResponse.builder()
                .message("User registered successfully")
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),
                        request.getPassword()));
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().toString());
        var accessToken = jwtUtil.generateToken(claims, user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        return LoginResponse.builder()
                .message("Logged in successfully.")
                .accessToken(accessToken)
                .build();
    }

    private void saveUserToken(User user, String accessToken) {
        var token = Token.builder()
                .user(user)
                .token(accessToken)
                .tokenType(BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtUtil.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail).orElseThrow();
            if (jwtUtil.isTokenValid(refreshToken, user)) {
                var accessToken = jwtUtil.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = LoginResponse.builder()
                        .message("New access token generated successfully.")
                        .accessToken(accessToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    @Override
    public boolean mobileNumberExists(String mobileNumber) {
        return userRepository.existsByMobilenumber(mobileNumber);
    }

    @Override
public UserDetailsResponse getUserDetailsByEmail(String email) {
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isPresent()) {
        User user = userOptional.get();
        // Convert User entity to UserDetailsResponse
        return UserDetailsResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .mobilenumber(user.getMobilenumber())
                .age(user.getAge())
                .address(user.getAddress())
                .state(user.getState())
                .city(user.getCity())
                .postalcode(user.getPostalcode())
                .skills(user.getSkills())
                .role(user.getRole().toString()) // Convert Role enum to String
                .build();
    } else {
        throw new RuntimeException("User not found for email: " + email);
    }
}

@Override
    public RegisterResponse updateUserDetails(RegisterRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Update user details
            user.setName(request.getName());
            user.setAge(request.getAge());
            user.setAddress(request.getAddress());
            user.setState(request.getState());
            user.setCity(request.getCity());
            user.setPostalcode(request.getPostalcode());
            user.setSkills(request.getSkills());
            userRepository.save(user);
            return RegisterResponse.builder()
                    .message("User details updated successfully")
                    .build();
        } else {
            throw new RuntimeException("User not found for email: " + request.getEmail());
        }
    }

}
