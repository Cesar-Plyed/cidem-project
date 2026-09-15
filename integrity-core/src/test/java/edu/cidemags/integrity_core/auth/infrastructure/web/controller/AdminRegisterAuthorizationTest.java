package edu.cidemags.integrity_core.auth.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cidemags.integrity_core.auth.application.usecase.LoginUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.LogoutUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.RefreshAccessTokenUseCase;
import edu.cidemags.integrity_core.auth.application.usecase.RegisterUserUseCase;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.infrastructure.security.SecurityConfig;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AdminRegisterAuthorizationTest {

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

    private RegisterRequest sampleRequest() {
        return new RegisterRequest("Admin", "Nuevo", "nuevo.admin@example.com", "password123",
                UUID.randomUUID(), null, Role.ADMINISTRATIVO, null);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void adminRegister_should_return_201_when_caller_has_administrativo_role() throws Exception {
        Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");
        User createdUser = new User(UUID.randomUUID(), "Admin", "Nuevo", "nuevo.admin@example.com",
                "hash", country, new ArrayList<>(), Role.ADMINISTRATIVO);

        when(registerUserUseCase.executeAsAdmin(any())).thenReturn(createdUser);

        mockMvc.perform(post("/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PROFESOR")
    void adminRegister_should_return_403_when_caller_does_not_have_administrativo_role() throws Exception {
        mockMvc.perform(post("/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ALUMNO")
    void adminRegister_should_return_403_when_caller_is_a_student() throws Exception {
        mockMvc.perform(post("/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRegister_should_reject_the_request_when_not_authenticated_at_all() throws Exception {
        mockMvc.perform(post("/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().is4xxClientError());
    }
}