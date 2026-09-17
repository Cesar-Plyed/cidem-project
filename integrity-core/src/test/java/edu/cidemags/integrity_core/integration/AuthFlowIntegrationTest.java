package edu.cidemags.integrity_core.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

        private static final String POSTGRES_IMAGE = "postgres:16-alpine";

        @Container
        @ServiceConnection
        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                        .withDatabaseName("integrity_core_test")
                        .withUsername("testuser")
                        .withPassword("testpass");

        @DynamicPropertySource
        static void registerProperties(DynamicPropertyRegistry registry) {
                registry.add("security.jwt.secret", () -> "integration-test-secret-of-at-least-32-chars!!");
                registry.add("security.jwt.access-token-expiration-minutes", () -> "15");
                registry.add("security.jwt.refresh-token-expiration-days", () -> "30");
                registry.add("security.registration-codes.administrativo", () -> "admin-test-code");
                registry.add("security.registration-codes.profesor", () -> "profesor-test-code");
                registry.add("security.rate-limit.max-attempts", () -> "1000");
                registry.add("security.rate-limit.window-seconds", () -> "600");
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private CountryJpaRepository countryJpaRepository;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private UUID countryId;

        @BeforeEach
        void seedCountry() {
                countryJpaRepository.deleteAll();
                CountryEntity saved = countryJpaRepository.save(new CountryEntity(null, "Mexico", "MEX"));
                countryId = saved.getId();
        }

        private String registerRequest(String email, String role, String registrationCode) {
                return """
                                {
                                  "firstNames": "Test",
                                  "lastNames": "User",
                                  "email": "%s",
                                  "password": "password123",
                                  "countryId": "%s",
                                  "phoneNumber": "22222222",
                                  "role": "%s",
                                  "registrationCode": "%s"
                                }
                                """
                                .formatted(email, countryId, role, registrationCode == null ? "" : registrationCode);
        }

        @Test
        void full_auth_flow_should_work_end_to_end_against_a_real_database() throws Exception {
                String email = "integration-" + UUID.randomUUID() + "@example.com";
                String registerBody = registerRequest(email, "ALUMNO", null);

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value(email));

                String loginBody = """
                                {"email": "%s", "password": "password123"}
                                """.formatted(email);

                String loginResponseJson = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode loginResponse = objectMapper.readTree(loginResponseJson);
                String firstRefreshToken = loginResponse.get("refreshToken").asText();

                String refreshBody = """
                                {"refreshToken": "%s"}
                                """.formatted(firstRefreshToken);

                String refreshResponseJson = mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshBody))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode refreshResponse = objectMapper.readTree(refreshResponseJson);
                String secondRefreshToken = refreshResponse.get("refreshToken").asText();

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshBody))
                                .andExpect(status().isUnauthorized());

                String logoutBody = """
                                {"refreshToken": "%s"}
                                """.formatted(secondRefreshToken);

                mockMvc.perform(post("/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(logoutBody))
                                .andExpect(status().isNoContent());

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(logoutBody))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void register_should_return_409_when_the_email_is_already_taken_in_the_real_database() throws Exception {
                String email = "duplicado-" + UUID.randomUUID() + "@example.com";
                String registerBody = registerRequest(email, "ALUMNO", null);

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody))
                                .andExpect(status().isConflict());
        }

        @Test
        void login_should_return_401_when_password_is_wrong() throws Exception {
                String email = "wrong-password-" + UUID.randomUUID() + "@example.com";
                String registerBody = registerRequest(email, "ALUMNO", null);

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody))
                                .andExpect(status().isCreated());

                String loginBody = """
                                {"email": "%s", "password": "wrong-password"}
                                """.formatted(email);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void refresh_should_return_401_when_refresh_token_is_invalid() throws Exception {
                String invalidRefreshToken = "invalid-token-123";

                String refreshBody = """
                                {"refreshToken": "%s"}
                                """.formatted(invalidRefreshToken);

                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshBody))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_should_return_401_when_refresh_token_is_invalid() throws Exception {
                String invalidRefreshToken = "not-a-valid-refresh-token";

                String logoutBody = """
                                {"refreshToken": "%s"}
                                """.formatted(invalidRefreshToken);

                mockMvc.perform(post("/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(logoutBody))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void register_should_return_400_when_registration_code_is_invalid_for_admin_role() throws Exception {
                String email = "invalid-registration-code-" + UUID.randomUUID() + "@example.com";
                String registerBody = registerRequest(email, "ADMINISTRATIVO", "wrong-code");

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody))
                                .andExpect(status().isForbidden());
        }

        @Test
        void register_should_accept_valid_registration_code_for_admin_role() throws Exception {
                String email = "valid-registration-code-" + UUID.randomUUID() + "@example.com";
                String registerBody = registerRequest(email, "ADMINISTRATIVO", "admin-test-code");

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value(email));
        }
}
