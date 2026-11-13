package edu.cidemags.integrity_core.legal.infrastructure.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.service.AuditService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.domain.annotation.AuditTrail;
import edu.cidemags.integrity_core.legal.domain.model.ActorRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest {

    @Mock
    private AuditService auditService;

    @Mock
    private LegalActorService actors;

    static class StubController {
        @AuditTrail(action = "START_EXAM", entityType = "EXAM")
        public void startExam() {
        }

        public void notAudited() {
        }
    }

    @Test
    void annotatedMethodWritesAuditLogWithPreviousAndCurrentState() throws Exception {
        UUID actorId = UUID.randomUUID();
        when(actors.currentActorId()).thenReturn(actorId);
        when(actors.currentActorRole()).thenReturn(ActorRole.STUDENT);

        AuditInterceptor interceptor = new AuditInterceptor(auditService, actors);
        HandlerMethod handler = handler("startExam");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/legal/consent/grant");
        request.addParameter("entityId", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        interceptor.postHandle(request, response, handler, null);
        interceptor.afterCompletion(request, response, handler, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> previousCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> currentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditService).record(eq(actorId), eq(ActorRole.STUDENT), eq("START_EXAM"), eq("EXAM"),
                any(), previousCaptor.capture(), currentCaptor.capture(), any(), any());

        assertThat(previousCaptor.getValue()).containsEntry("path", "/api/legal/consent/grant")
                .containsEntry("method", "POST");
        assertThat(currentCaptor.getValue()).containsEntry("status", 201)
                .containsEntry("success", true);
        assertThat(previousCaptor.getValue()).isNotEqualTo(currentCaptor.getValue());
    }

    @Test
    void unannotatedMethodIsIgnored() throws Exception {
        AuditInterceptor interceptor = new AuditInterceptor(auditService, actors);
        HandlerMethod handler = handler("notAudited");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/legal/consent");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.postHandle(request, response, handler, null);
        interceptor.afterCompletion(request, response, handler, null);

        verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        StubController controller = new StubController();
        return new HandlerMethod(controller, StubController.class.getMethod(methodName));
    }
}
