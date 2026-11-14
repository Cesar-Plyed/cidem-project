package edu.cidemags.integrity_core.legal.infrastructure.config;

import edu.cidemags.integrity_core.legal.application.service.AuditService;
import edu.cidemags.integrity_core.legal.application.service.LegalActorService;
import edu.cidemags.integrity_core.legal.infrastructure.interceptor.AuditInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion del modulo legal: registra el audit interceptor (spec 39).
 *
 * <p>El interceptor se declara aqui, condicionado a que exista
 * {@link AuditService}. Los tests de slice ({@code @WebMvcTest}) cargan esta
 * configuracion pero no los servicios, asi que el bean no se crea y el
 * contexto arranca sin auditoria.</p>
 */
@Configuration
public class LegalConfig implements WebMvcConfigurer {

    private final ObjectProvider<AuditInterceptor> auditInterceptor;

    public LegalConfig(ObjectProvider<AuditInterceptor> auditInterceptor) {
        this.auditInterceptor = auditInterceptor;
    }

    @Bean
    @ConditionalOnBean(AuditService.class)
    public AuditInterceptor auditInterceptor(AuditService auditService, LegalActorService actors) {
        return new AuditInterceptor(auditService, actors);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        AuditInterceptor interceptor = auditInterceptor.getIfAvailable();
        if (interceptor != null) {
            registry.addInterceptor(interceptor).addPathPatterns("/api/legal/**");
        }
    }
}
