package edu.cidemags.integrity_core.legal.infrastructure.interceptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.service.AuditService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.domain.annotation.AuditTrail;
import edu.cidemags.integrity_core.legal.domain.model.ActorRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor de auditoria (spec 39): toda accion anotada con {@link AuditTrail}
 * deja una fila en {@code audit_logs} con actor, accion, estado previo/actual
 * e IP/user-agent.
 *
 * <p>No lleva {@code @Component} a propósito: los tests de slice
 * ({@code @WebMvcTest}) incluyen cualquier {@code HandlerInterceptor} y fallaban
 * al no existir {@code AuditService}. Lo declara {@code LegalConfig}, que sí
 * puede condicionarlo a que el servicio esté presente.</p>
 */
public class AuditInterceptor implements HandlerInterceptor {

    private static final String PREVIOUS_STATE_ATTRIBUTE = AuditInterceptor.class.getName() + ".previous";

    private final AuditService auditService;
    private final LegalActorService actors;

    public AuditInterceptor(AuditService auditService, LegalActorService actors) {
        this.auditService = auditService;
        this.actors = actors;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
        if (resolve(handler) != null) {
            request.setAttribute(PREVIOUS_STATE_ATTRIBUTE, snapshotArguments(request));
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuditTrail annotation = resolve(handler);
        if (annotation == null) {
            return;
        }
        Map<String, Object> previous = previousState(request);
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("status", response.getStatus());
        current.put("success", ex == null && response.getStatus() < 400);
        current.put("exception", ex == null ? null : ex.getClass().getSimpleName());

        UUID actorId;
        ActorRole actorRole;
        try {
            actorId = actors.currentActorId();
            actorRole = actors.currentActorRole();
        } catch (RuntimeException securityOrParseError) {
            actorId = null;
            actorRole = ActorRole.SYSTEM;
        }

        UUID entityId = extractEntityId(request);
        auditService.record(actorId, actorRole, annotation.action(), annotation.entityType(), entityId,
                previous, current, clientIp(request), request.getHeader("User-Agent"));
    }

    static AuditTrail resolve(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getMethodAnnotation(AuditTrail.class);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> previousState(HttpServletRequest request) {
        Object attribute = request.getAttribute(PREVIOUS_STATE_ATTRIBUTE);
        return attribute instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * Estado previo observable sin acoplarse al HandlerAdapter: ruta y
     * parametros de la peticion. El estado actual lo aporta la respuesta
     * (status/success/exception); ambos quedan en la misma fila de audit_logs.
     */
    private static Map<String, Object> snapshotArguments(HttpServletRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("method", request.getMethod());
        snapshot.put("path", request.getRequestURI());
        request.getParameterMap().forEach((key, values) ->
                snapshot.put(key, values.length == 1 ? values[0] : values));
        return snapshot;
    }

    private static UUID extractEntityId(HttpServletRequest request) {
        for (String candidate : new String[]{request.getParameter("entityId"), request.getParameter("id")}) {
            if (candidate != null) {
                try {
                    return UUID.fromString(candidate);
                } catch (IllegalArgumentException ignored) {
                    // sigue buscando
                }
            }
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
