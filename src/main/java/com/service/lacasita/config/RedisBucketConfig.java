package com.service.lacasita.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisBucketConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisBucketConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient() {
        return RedisClient.create("redis://" + host + ":" + port);
    }

    @Bean
    public ProxyManager<String> proxyManager(RedisClient rateLimitRedisClient) {

        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

        StatefulRedisConnection<String, byte[]> connection;
        try {
            connection = rateLimitRedisClient.connect(codec);
        } catch (RedisConnectionException e) {
            log.warn("Redis no disponible al arrancar; rate limit desactivado hasta reiniciar: {}", e.getMessage());
            return null;
        }

        RedisAsyncCommands<String, byte[]> commands = connection.async();

        return LettuceBasedProxyManager
                .builderFor(commands)
                // Los buckets ociosos se descartan en vez de acumularse en Redis.
                .withExpirationStrategy(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }

    /**
     * Limpia la caché al arrancar. Todo lo que cabe en {@code *} es recalculable
     * —dashboards, permisos, categorías— y perderlo no cuesta nada.
     */
    @Bean
    public CommandLineRunner clearCachesOnStartup(StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                var keys = redisTemplate.keys("*");
                if (keys != null) {
                    keys.removeIf(k -> k.startsWith("notif:"));
                }
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                // Si Redis no está disponible al iniciar, no bloquear la app.
                log.warn("No se pudo limpiar la caché de Redis al arrancar: {}", e.getMessage());
            }
        };
    }

}
