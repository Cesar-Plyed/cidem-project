package edu.cidemags.integrity_core.safeguard.domain.port;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;

/** Restricciones configurables de SafeGuard por examen (spec 19). */
public interface RestrictionRepository {

    List<Restriction> findByExamId(UUID examId);

    /** Reemplaza la lista completa de restricciones del examen. */
    List<Restriction> replaceForExam(UUID examId, List<Restriction> restrictions);
}
