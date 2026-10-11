package edu.cidemags.integrity_core.oversight.ports.in;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;

public interface RecordLogEntryUseCase {

    IntegritySnapshot record(RecordLogEntryCommand command);
}