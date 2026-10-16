package com.service.lacasita.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * Bloqueo por cuenta ante intentos de login fallidos
 */
@Service
public class AccountLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockoutService.class);

    private final ObjectProvider<ProxyManager<String>> proxyManagerProvider;
    private final boolean enabled;
    private final String keyPrefix;
    private final long maxAttempts;
    private final Duration lockoutDuration;

    public AccountLockoutService(
            ObjectProvider<ProxyManager<String>> proxyManagerProvider,
            @Value("${app.account-lockout.enabled:true}") boolean enabled,
            @Value("${app.account-lockout.key-prefix:lacasita:account-lock:}") String keyPrefix,
            @Value("${app.account-lockout.max-attempts:5}") long maxAttempts,
            @Value("${app.account-lockout.lockout-duration:PT15M}") Duration lockoutDuration) {

        this.proxyManagerProvider = proxyManagerProvider;
        this.enabled              = enabled;
        this.keyPrefix            = keyPrefix;
        this.maxAttempts          = maxAttempts;
        this.lockoutDuration      = lockoutDuration;
    }

    private Bucket bucketFor(ProxyManager<String> proxyManager, String username) {
        Supplier<BucketConfiguration> config = () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(maxAttempts)
                                .refillGreedy(maxAttempts, lockoutDuration)
                                .build())
                        .build();
        return proxyManager.builder().build(keyPrefix + username.toLowerCase(), config);
    }

    /**
     * Consume un intento del cupo de la cuenta;
     */
    public boolean tryAcquire(String username) {

        if (!enabled) {
            return true;
        }

        ProxyManager<String> proxyManager = proxyManagerProvider.getIfAvailable();
        if (proxyManager == null) {
            return true;
        }

        try {
            return bucketFor(proxyManager, username).tryConsume(1);
        } catch (Exception e) {
            log.warn("No se pudo verificar el bloqueo de cuenta, se deja pasar: {}", e.getMessage());
            return true;
        }
    }

    /** Login exitoso: limpia los intentos fallidos previos de la cuenta. */
    public void reset(String username) {

        if (!enabled) {
            return;
        }

        ProxyManager<String> proxyManager = proxyManagerProvider.getIfAvailable();
        if (proxyManager == null) {
            return;
        }

        try {
            proxyManager.removeProxy(keyPrefix + username.toLowerCase());
        } catch (Exception e) {
            log.warn("No se pudo resetear el contador de intentos fallidos: {}", e.getMessage());
        }
    }
}
