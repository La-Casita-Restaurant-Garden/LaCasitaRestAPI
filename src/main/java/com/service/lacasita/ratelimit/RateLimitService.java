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
 * Control de tasa por clave (un bucket por IP de cliente).
 *
 * <p>Los buckets viven en Redis para que el límite se respete entre instancias. Las
 * claves llevan prefijo porque esa instancia de Redis se comparte con el servicio
 * guest.
 *
 * <p><b>Falla abierto.</b> Si Redis no está configurado o está caído, las peticiones
 * pasan. Es una decisión deliberada: el rate limiting protege contra abuso, pero no
 * es una función del negocio — que se caiga Redis no puede dejar al restaurante sin
 * poder tomar pedidos.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final ObjectProvider<ProxyManager<String>> proxyManagerProvider;
    private final boolean enabled;
    private final String keyPrefix;
    private final long capacity;
    private final Duration refillPeriod;

    public RateLimitService(
            ObjectProvider<ProxyManager<String>> proxyManagerProvider,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.key-prefix:lacasita:rate-limit:}") String keyPrefix,
            @Value("${app.rate-limit.capacity:60}") long capacity,
            @Value("${app.rate-limit.refill-period:PT1M}") Duration refillPeriod) {

        this.proxyManagerProvider = proxyManagerProvider;
        this.enabled = enabled;
        this.keyPrefix = keyPrefix;
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
    }

    /** Peticiones permitidas por periodo; el filtro la usa para el header. */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Consume un token para la clave dada.
     *
     * @return {@code true} si la petición se permite. También {@code true} cuando el
     *         control de tasa está deshabilitado o Redis no está disponible.
     */
    public boolean allowRequest(String key) {

        if (!enabled) {
            return true;
        }

        ProxyManager<String> proxyManager = proxyManagerProvider.getIfAvailable();
        if (proxyManager == null) {
            return true;
        }

        Supplier<BucketConfiguration> config = () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(capacity, refillPeriod)
                                .build())
                        .build();

        try {
            Bucket bucket = proxyManager.builder().build(keyPrefix + key, config);
            return bucket.tryConsume(1);
        } catch (Exception e) {
            log.warn("Control de tasa no disponible, se deja pasar la petición: {}", e.getMessage());
            return true;
        }
    }
}
