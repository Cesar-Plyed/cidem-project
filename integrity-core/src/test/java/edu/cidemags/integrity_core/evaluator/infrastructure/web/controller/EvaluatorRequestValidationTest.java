package edu.cidemags.integrity_core.evaluator.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.infrastructure.security.SecurityConfig;
import edu.cidemags.integrity_core.evaluator.application.usecase.CreateExamUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.CreateQuestionBankUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ExamManagementUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ExamQuestionManagementUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ExportExamResultsUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GenerateExamAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetAttemptExamUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.GetExamResultUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.JoinExamByAccessCodeUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ListOpenEndedReviewsUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.QuestionBankManagementUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.ReviewOpenEndedAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitExamAttemptUseCase;
import edu.cidemags.integrity_core.evaluator.application.usecase.SubmitQuestionAnswerUseCase;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C2: el modulo evaluator valida el cuerpo de las peticiones con Bean Validation
// (@Valid en los controladores). Se prueba con el filtro JWT real para que el
// controlador reciba un Authentication con principal UUID.
@WebMvcTest(controllers = { ExamController.class, ExamAttemptController.class, QuestionBankController.class,
        EvaluationReviewController.class })
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class EvaluatorRequestValidationTest {

    private static final String TOKEN = "token-de-prueba";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TokenProvider tokenProvider;
    @MockitoBean
    private CreateExamUseCase createExamUseCase;
    @MockitoBean
    private GenerateExamAccessCodeUseCase generateExamAccessCodeUseCase;
    @MockitoBean
    private ExamManagementUseCase examManagementUseCase;
    @MockitoBean
    private ExamQuestionManagementUseCase questionManagementUseCase;
    @MockitoBean
    private GetExamResultUseCase getExamResultUseCase;
    @MockitoBean
    private ExportExamResultsUseCase exportExamResultsUseCase;
    @MockitoBean
    private JoinExamByAccessCodeUseCase joinExamByAccessCodeUseCase;
    @MockitoBean
    private SubmitExamAttemptUseCase submitExamAttemptUseCase;
    @MockitoBean
    private SubmitQuestionAnswerUseCase submitQuestionAnswerUseCase;
    @MockitoBean
    private GetAttemptExamUseCase getAttemptExamUseCase;
    @MockitoBean
    private CreateQuestionBankUseCase createQuestionBankUseCase;
    @MockitoBean
    private QuestionBankManagementUseCase questionBankManagementUseCase;
    @MockitoBean
    private ReviewOpenEndedAnswerUseCase reviewOpenEndedAnswerUseCase;
    @MockitoBean
    private ListOpenEndedReviewsUseCase listOpenEndedReviewsUseCase;

    private void givenProfessor() {
        when(tokenProvider.validateAccessToken(TOKEN)).thenReturn(
                new TokenProvider.AccessTokenClaims(UUID.randomUUID(), "profesor@test.com", Role.PROFESOR));
    }

    // ---------- Examenes ----------

    @Test
    void createExam_returns_400_when_title_is_blank() throws Exception {
        givenProfessor();

        mockMvc.perform(post("/api/evaluator/exams").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.message").value("El titulo del examen no puede estar vacio"));
    }

    @Test
    void createExam_returns_400_when_question_type_is_missing() throws Exception {
        givenProfessor();

        String body = objectMapper.writeValueAsString(java.util.Map.of("title", "Examen valido",
                "questions", java.util.List.of(java.util.Map.of("prompt", "Pregunta sin tipo"))));

        mockMvc.perform(post("/api/evaluator/exams").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void createExam_returns_201_when_payload_is_valid() throws Exception {
        givenProfessor();
        when(createExamUseCase.execute(any())).thenReturn(new Exam("Examen valido", "desc", UUID.randomUUID()));

        mockMvc.perform(post("/api/evaluator/exams").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Examen valido\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Examen valido"));
    }

    @Test
    void updateExam_returns_400_when_title_is_blank() throws Exception {
        givenProfessor();

        mockMvc.perform(put("/api/evaluator/exams/{examId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    // ---------- Bancos de preguntas ----------

    @Test
    void createQuestionBank_returns_400_when_name_is_blank() throws Exception {
        givenProfessor();

        mockMvc.perform(post("/api/evaluator/question-banks").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // ---------- Intentos de examen ----------

    @Test
    void joinByCode_returns_400_when_code_is_blank() throws Exception {
        givenProfessor();

        mockMvc.perform(post("/api/evaluator/attempts/join-by-code").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists());
    }

    @Test
    void submitAnswer_returns_400_when_question_id_is_missing() throws Exception {
        givenProfessor();

        mockMvc.perform(post("/api/evaluator/attempts/{attemptId}/answers", UUID.randomUUID())
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openTextAnswer\":\"respuesta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.questionId").exists());
    }

    // ---------- Revisiones ----------

    @Test
    void reviewOpenEnded_returns_400_when_score_is_negative() throws Exception {
        givenProfessor();

        mockMvc.perform(post("/api/evaluator/reviews/open-ended").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerId\":\"" + UUID.randomUUID() + "\",\"score\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.score").exists());
    }
}
