package edu.cidemags.integrity_core.safeguard.application.listener;

import edu.cidemags.integrity_core.shared.events.ExamAttemptRevokedEvent;
import edu.cidemags.integrity_core.shared.events.IntegrityThresholdBreachedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotifyStudentOnIntegrityFailureListener {

    private final SimpMessagingTemplate messagingTemplate;

    public NotifyStudentOnIntegrityFailureListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onIntegrityThresholdBreached(IntegrityThresholdBreachedEvent event) {
        notifyStudent(event.attemptId(), event.studentId(), "INTEGRITY_FAILURE");
    }

    @TransactionalEventListener
    public void onAttemptRevoked(ExamAttemptRevokedEvent event) {
        notifyStudent(event.attemptId(), event.studentId(), "TEACHER_REVOKED");
    }

    private void notifyStudent(java.util.UUID attemptId, java.util.UUID studentId, String reason) {
        messagingTemplate.convertAndSendToUser(
                studentId.toString(),
                "/queue/exam-status",
                new ExamTerminatedNotification(attemptId, reason));
    }

    public record ExamTerminatedNotification(java.util.UUID attemptId, String reason) {
    }
}
