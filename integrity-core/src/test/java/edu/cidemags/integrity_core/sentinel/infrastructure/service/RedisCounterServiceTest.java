package edu.cidemags.integrity_core.sentinel.infrastructure.service;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.model.EscalationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RedisCounterServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisCounterService counterService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        counterService = new RedisCounterService(redisTemplate);
    }

    @Test
    void shouldActivateLowRuleAfterThreeTabSwitches() {
        EscalationRule rule = new EscalationRule("TAB_SWITCH", "TAB_OR_APP_SWITCH", 3,
            Duration.ofMinutes(5), AlertSeverity.LOW);
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L, 3L);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(300L);

        assertThat(counterService.evaluate(rule)).isFalse();
        assertThat(counterService.evaluate(rule)).isFalse();
        assertThat(counterService.evaluate(rule)).isTrue();
    }

    @Test
    void shouldActivateHighRuleAfterTwoDisconnections() {
        EscalationRule rule = new EscalationRule("DISCONNECTION", "DISCONNECTION", 2,
            Duration.ofMinutes(5), AlertSeverity.HIGH);
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(300L);

        assertThat(counterService.evaluate(rule)).isFalse();
        assertThat(counterService.evaluate(rule)).isTrue();
    }

    @Test
    void shouldResetCounterAndDeactivateRule() {
        EscalationRule rule = new EscalationRule("TAB_SWITCH", "TAB_OR_APP_SWITCH", 3,
            Duration.ofMinutes(5), AlertSeverity.LOW);
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(-2L);

        assertThat(counterService.evaluate(rule)).isTrue();

        counterService.reset(rule.eventType());
        verify(redisTemplate).delete("sentinel:counter:TAB_OR_APP_SWITCH");
        assertThat(counterService.current(rule.eventType())).isZero();
        assertThat(counterService.ttl(rule.eventType())).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldApplyWindowTtlOnFirstIncrement() {
        Duration window = Duration.ofSeconds(60);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(60L);

        RedisCounterService.Counter counter = counterService.increment("WINDOW_FOCUS_LOST", window);

        assertThat(counter.value()).isEqualTo(1);
        assertThat(counter.ttl()).isEqualTo(window);
        verify(redisTemplate).expire("sentinel:counter:WINDOW_FOCUS_LOST", window);
    }
}
