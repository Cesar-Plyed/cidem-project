package edu.cidemags.integrity_core.auth.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cidemags.integrity_core.auth.application.usecase.GenerateRegistrationCodeUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.LoginUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.LogoutUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.RefreshAccessTokenUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.RegisterUserUseCase;
import edu.cidemags.integrity_core.auth.domain.exception.CountryNotFoundException;
import edu.cidemags.integrity_core.auth.domain.exception.EmailAlreadyRegisteredException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidCredentialsException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidRefreshTokenException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidRoleAssignmentException;
import edu.cidemags.integrity_core.auth.domain.exception.UserNotFoundException;
import edu.cidemags.integrity_core.auth.domain.model.AuthTokens;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.GenerateRegistrationCodeRequest;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.LoginRequest;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.RefreshTokenRequest;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.RegisterRequest;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private RegisterUserUseCase registerUserUseCase;
        @MockitoBean
        private LoginUseCase loginUseCase;
        @MockitoBean
        private RefreshAccessTokenUseCase refreshAccessTokenUseCase;
        @MockitoBean
        private LogoutUseCase logoutUseCase;
        @MockitoBean
        private TokenProvider tokenProvider;
        @MockitoBean
        private GenerateRegistrationCodeUseCase generateRegistrationCodeUseCase;

        private User sampleUser() {
                Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");
                return new User(UUID.randomUUID(), "Armando", "Campos", "armando.campos@gmail.com",
                                "imagine-a-hased-password",
                                country, new ArrayList<>(), Role.ALUMNO);
        }

        // Register Tests

        @Test
        void register_should_return_201_and_the_user_created_without_password() throws Exception {
                RegisterRequest request = new RegisterRequest("Armando", "Campos", "armando.campos@gmail.com",
                                "arroz123",
                                UUID.randomUUID(), "4444444444", Role.ALUMNO, null);

                when(registerUserUseCase.execute(any())).thenReturn(sampleUser());

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("armando.campos@gmail.com"))
                                .andExpect(jsonPath("$.role").value("ALUMNO"))
                                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        void register_should_return_409_if_the_email_alredy_exists() throws Exception {
                RegisterRequest request = new RegisterRequest("Armando", "Campos", "armando.campos@gmail.com",
                                "arroz123",
                                UUID.randomUUID(), null, Role.ALUMNO, null);

                when(registerUserUseCase.execute(any()))
                                .thenThrow(new EmailAlreadyRegisteredException("armando.campos@gmail.com"));

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.message")
                                                .value(containsStringIgnoringCase("armando.campos@gmail.com")));
        }

        @Test
        void register_should_return_409_on_data_integrity_violation() throws Exception {
                RegisterRequest request = new RegisterRequest("Armando", "Campos", "armando.campos@gmail.com",
                                "arroz123",
                                UUID.randomUUID(), null, Role.ALUMNO, null);

                when(registerUserUseCase.execute(any()))
                                .thenThrow(new DataIntegrityViolationException(
                                                "duplicate key value violates unique constraint"));

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        void register_should_return_403_if_the_rol_code_is_invalid() throws Exception {
                RegisterRequest request = new RegisterRequest("Juan", "Perez", "juan@example.com", "password123",
                                UUID.randomUUID(), null, Role.PROFESOR, "bad-code");

                when(registerUserUseCase.execute(any())).thenThrow(
                                new InvalidRoleAssignmentException(
                                                "Codigo de registro invalido o faltante para el rol Profesor"));

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void register_should_return_400_with_fieldErrors_if_email_missing() throws Exception {
                RegisterRequest request = new RegisterRequest("Nestor", "Perez", "", "password123", UUID.randomUUID(),
                                null,
                                Role.ALUMNO, null);

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        void register_should_return_404_when_the_country_does_not_exixst() throws Exception {
                RegisterRequest request = new RegisterRequest("Nestor", "Perez", "nestor.perez@exapmle.com",
                                "password123", UUID.randomUUID(),
                                null,
                                Role.ALUMNO, null);

                UUID missingCountryId = UUID.randomUUID();

                when(registerUserUseCase.execute(any())).thenThrow(new CountryNotFoundException(missingCountryId));

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        void register_should_return_400_when_a_domain_invariant_is_violated() throws Exception {
                RegisterRequest request = new RegisterRequest("Nestor", "Perez", "nestor.perez@exapmle.com",
                                "password123", UUID.randomUUID(),
                                null,
                                Role.ALUMNO, null);

                when(registerUserUseCase.execute(any()))
                                .thenThrow(new IllegalArgumentException("El pais no puede estar vacio"));

                mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // Login Tests

        @Test
        void login_should_return_200_with_the_tokens() throws Exception {
                LoginRequest request = new LoginRequest("armando.campos@gmail.com", "arroz123");

                when(loginUseCase.execute(any())).thenReturn(new AuthTokens("fake-access-token", "fake-refresh-token"));

                mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("fake-access-token"))
                                .andExpect(jsonPath("$.refreshToken").value("fake-refresh-token"));
        }

        @Test
        void login_should_return_401_with_invalid_credentials() throws Exception {
                LoginRequest request = new LoginRequest("armando.campos@gmail.com", "password-incorrecto");

                when(loginUseCase.execute(any())).thenThrow(new InvalidCredentialsException());

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Correo o contraseña invalidos"));
        }

        @Test
        void login_should_not_disclose_internal_details_if_an_unexpected_error_occurs() throws Exception {
                LoginRequest request = new LoginRequest("armando.campos@gmail.com", "arroz123");

                when(loginUseCase.execute(any()))
                                .thenThrow(new RuntimeException("Connection refused: tabla users, host 10.0.0.5:5432"));

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message")
                                                .value("Ocurrio un error inesperado. Intenta de nuevo mas tarde."))
                                .andExpect(content().string(org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("10.0.0.5"))));
        }

        @Test
        void login_should_return_400_when_email_is_blank() throws Exception {
                LoginRequest request = new LoginRequest("", "password123");

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        // Refresh Tests

        @Test
        void refresh_should_return_200_with_the_new_pair_of_tokens() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest("refresh-viejo");

                when(refreshAccessTokenUseCase.execute("refresh-viejo"))
                                .thenReturn(new AuthTokens("access-nuevo", "refresh-nuevo"));

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-nuevo"));
        }

        @Test
        void refresh_should_return_401_if_the_token_is_invalid() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest("refresh-invalido");

                when(refreshAccessTokenUseCase.execute("refresh-invalido"))
                                .thenThrow(new InvalidRefreshTokenException());

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void refresh_should_return_400_when_refresh_token_is_blank() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest("");

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.refreshToken").exists());
        }

        @Test
        void refresh_should_return_404_when_the_token_owner_no_longer_exists() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest("any-token");

                when(refreshAccessTokenUseCase.execute("any-token"))
                                .thenThrow(new UserNotFoundException(UUID.randomUUID()));

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // Logout Tests

        @Test
        void logout_should_return_204_without_body() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest("refresh-valido");

                mockMvc.perform(post("/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));
        }

        @Test
        void logoutAll_should_return_204_and_revoke_all_tokens_for_the_authenticated_user() throws Exception {
                UUID userId = UUID.randomUUID();
                Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());

                mockMvc.perform(post("/auth/logout-all").principal(authentication))
                                .andExpect(status().isNoContent());

                verify(logoutUseCase).executeForAllDevices(userId);
        }

        // Regsitration Codes
        @Test
        void generateRegistrationCode_should_return_201_with_the_generated_code() throws Exception {
                GenerateRegistrationCodeRequest request = new GenerateRegistrationCodeRequest(Role.PROFESOR, 7);
                RegistrationCode generated = new RegistrationCode(
                                "test-generated-code", Role.PROFESOR, OffsetDateTime.now().plusDays(7));

                when(generateRegistrationCodeUseCase.execute(any())).thenReturn(generated);

                mockMvc.perform(post("/auth/admin/registration-codes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.code").value("test-generated-code"))
                                .andExpect(jsonPath("$.role").value("PROFESOR"));
        }

        private static org.hamcrest.Matcher<String> containsStringIgnoringCase(String substring) {
                return org.hamcrest.Matchers.containsStringIgnoringCase(substring);
        }
}
