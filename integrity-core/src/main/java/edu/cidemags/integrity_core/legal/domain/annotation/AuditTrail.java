package edu.cidemags.integrity_core.legal.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint que debe quedar registrado en el audit trail (spec 39).
 *
 * <pre>
 * &#64;AuditTrail(action = "SAVE_RESPONSE", entityType = "RESPONSE")
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditTrail {

    String action();

    String entityType();
}
