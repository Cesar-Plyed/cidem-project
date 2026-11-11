package edu.cidemags.integrity_core.modules;

import java.util.Map;
import java.util.Set;

import edu.cidemags.integrity_core.modules.ModuleConfig.Module;

/**
 * Tabla de propiedades de cada módulo: módulos, tablas propietarias, tablas compartidas,
 * relaciones con otros módulos y diagrama de dependencias.
 *
 * <pre>
 *  [Auth] ──users──► [Exams]
 *   │                │
 *   │                ▼
 *   │           [Safeguard]
 *   │                ▲
 *   │                │
 *   ├──► [Sentinel] ◄─┤
 *   │                │
 *   ▼                │
 * [Oversight] ◄─────┘
 *   ▲
 *   │
 * [Evaluator]
 *
 * Ownership:
 *   AUTH     → users, countries, phone_numbers, refresh_tokens, registration_codes
 *   SAFEGUARD → safeguard_sessions, safeguard_events, safeguard_config, exam_attempts
 *   SENTINEL  → sentinel_suspicious_events, sentinel_alerts
 *   EVALUATOR → evaluator_exams, evaluator_exam_questions, evaluator_question_bank, evaluator_exam_scores
 *   OVERSIGHT → oversight_log_entries, oversight_integrity_snapshots, oversight_bitacora
 *
 * Shared tables (multiple modules read/write):
 *   exams, exam_sessions
 * </pre>
 */
public final class ModuleConfig {

    private ModuleConfig() {}

    public enum Module {
        AUTH("auth"),
        SAFEGUARD("safeguard"),
        SENTINEL("sentinel"),
        EVALUATOR("evaluator"),
        OVERSIGHT("oversight"),
        SHARED("shared");

        private final String name;

        Module(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }

    /** Tablas propietarias por módulo (no las comparte con otros). */
    public static final Map<Module, Set<String>> TABLE_OWNERSHIP = Map.of(
            Module.AUTH, Set.of(
                    "users",
                    "countries",
                    "phone_numbers",
                    "refresh_tokens",
                    "registration_codes"
            ),
            Module.SAFEGUARD, Set.of(
                    "safeguard_sessions",
                    "safeguard_events",
                    "safeguard_config",
                    "exam_attempts"
            ),
            Module.SENTINEL, Set.of(
                    "sentinel_suspicious_events",
                    "sentinel_alerts"
            ),
            Module.EVALUATOR, Set.of(
                    "evaluator_exams",
                    "evaluator_exam_questions",
                    "evaluator_question_bank",
                    "evaluator_exam_scores"
            ),
            Module.OVERSIGHT, Set.of(
                    "oversight_log_entries",
                    "oversight_integrity_snapshots",
                    "oversight_bitacora"
            )
    );

    /** Tablas compartidas entre múltiples módulos. */
    public static final Set<String> SHARED_TABLES = Set.of(
            "exams",
            "exam_sessions"
    );

    /** Relaciones entre módulos (quién usa a quién). */
    public static final Map<Module, Set<Module>> MODULE_DEPENDENCIES = Map.of(
            Module.SAFEGUARD, Set.of(Module.AUTH, Module.OVERSIGHT, Module.SENTINEL),
            Module.SENTINEL, Set.of(Module.SAFEGUARD, Module.OVERSIGHT),
            Module.OVERSIGHT, Set.of(Module.SAFEGUARD, Module.SENTINEL, Module.EVALUATOR),
            Module.EVALUATOR, Set.of(Module.AUTH, Module.OVERSIGHT)
    );

    /** Verifica que una tabla pertenezca a un módulo. */
    public static boolean owns(Module module, String tableName) {
        return TABLE_OWNERSHIP.getOrDefault(module, Set.of()).contains(tableName);
    }

    /** Verifica si una tabla es compartida. */
    public static boolean isShared(String tableName) {
        return SHARED_TABLES.contains(tableName);
    }

    /** Lista todas las tablas de un módulo (propias + compartidas). */
    public static Set<String> getAllTables(Module module) {
        Set<String> result = new java.util.HashSet<>(TABLE_OWNERSHIP.getOrDefault(module, Set.of()));
        result.addAll(SHARED_TABLES);
        return result;
    }
}
