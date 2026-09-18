package edu.cidemags.integrity_core.sentinel.ports.in;

import edu.cidemags.integrity_core.sentinel.domain.Alert;

import java.util.Optional;

public interface ReceiveSuspiciousEventUseCase {

    Optional<Alert> receive(ReceiveSuspiciousEventCommand command);
}