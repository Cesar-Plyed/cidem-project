package edu.cidemags.integrity_core.oversight.ports.in;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;

import java.util.Optional;
import java.util.UUID;

public interface GetAttemptIntegrityUseCase {
    // Optional.empty() puede significar "attemptId inexistente" o "intento sin eventos
    // aún registrados" — Oversight no valida contra evaluator, no puede distinguirlos.
    Optional<IntegritySnapshot> getIntegrity(UUID attemptId);
}