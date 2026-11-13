package edu.cidemags.integrity_core.legal.application.service;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ErasureRequest;
import edu.cidemags.integrity_core.legal.domain.model.ErasureType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ErasureRequestRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derecho de supresion (Art. 17 GDPR, spec 44).
 *
 * <p>El borrado hard elimina respuestas y alertas y pseudonimiza el audit
 * trail (el interestado desaparece de el, pero se conserva la traza legal).
 * El usuario no se elimina fisicamente porque exam_attempts lo referencia:
 * se marca {@code is_deleted = true}.</p>
 */
@Service
public class ErasureService {

    private static final Logger log = LoggerFactory.getLogger(ErasureService.class);

    private final StudentDataRepository studentData;
    private final ErasureRequestRepository requests;

    public ErasureService(StudentDataRepository studentData, ErasureRequestRepository requests) {
        this.studentData = studentData;
        this.requests = requests;
    }

    @Transactional
    public ErasureRequest eraseStudentData(UUID studentId) {
        ErasureRequest request = requests.save(new ErasureRequest(studentId, ErasureType.HARD));
        try {
            int responses = studentData.deleteResponsesByStudent(studentId);
            int alerts = studentData.deleteAlerts(studentId);
            int anonymized = studentData.anonymizeAuditLogs(studentId);
            studentData.softErase(studentId);
            request.complete();
            log.info("Borrado hard de {}: {} respuestas, {} alertas, {} logs pseudonimizados",
                    studentId, responses, alerts, anonymized);
        } catch (RuntimeException ex) {
            request.fail();
            requests.save(request);
            throw ex;
        }
        return requests.save(request);
    }

    @Transactional
    public ErasureRequest softEraseStudentData(UUID studentId) {
        ErasureRequest request = requests.save(new ErasureRequest(studentId, ErasureType.SOFT));
        try {
            studentData.softErase(studentId);
            request.complete();
        } catch (RuntimeException ex) {
            request.fail();
            requests.save(request);
            throw ex;
        }
        return requests.save(request);
    }

    @Transactional(readOnly = true)
    public boolean isErased(UUID studentId) {
        return !studentData.hasAnyData(studentId);
    }
}
