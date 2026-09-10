package edu.cidemags.integrity_core.auth.infrastructure.web.controller;

import edu.cidemags.integrity_core.auth.application.dto.LoginCommand;
import edu.cidemags.integrity_core.auth.application.dto.RegisterUserCommand;
import edu.cidemags.integrity_core.auth.application.usecase.LoginUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.LogoutUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.RefreshAccessTokenUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.RegisterUserUseCase;
import edu.cidemags.integrity_core.auth.domain.model.AuthTokens;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          LoginUseCase loginUseCase,
                          RefreshAccessTokenUseCase refreshAccessTokenUseCase,
                          LogoutUseCase logoutUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.firstNames(),
                request.lastNames(),
                request.email(),
                request.password(),
                request.countryId(),
                request.phoneNumber(),
                request.role(),
                request.registrationCode()
        );
        User user = registerUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens tokens = refreshAccessTokenUseCase.execute(request.refreshToken());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logoutUseCase.execute(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMINISTRATIVO')")
    public ResponseEntity<UserResponse> registerAsAdmin(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.firstNames(),
                request.lastNames(),
                request.email(),
                request.password(),
                request.countryId(),
                request.phoneNumber(),
                request.role(),
                request.registrationCode()
        );
        User user = registerUserUseCase.executeAsAdmin(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }
}