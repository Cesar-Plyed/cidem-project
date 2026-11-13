package edu.cidemags.integrity_core.legal.infrastructure.config;

import edu.cidemags.integrity_core.legal.infrastructure.interceptor.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configuracion del modulo legal: registra el audit interceptor (spec 39). */
@Configuration
public class LegalConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;

    public LegalConfig(AuditInterceptor auditInterceptor) {
        this.auditInterceptor = auditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor).addPathPatterns("/api/legal/**");
    }
}
