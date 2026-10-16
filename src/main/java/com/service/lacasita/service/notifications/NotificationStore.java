package com.service.lacasita.service.notifications;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.service.lacasita.entity.Notification;

import lombok.RequiredArgsConstructor;

/**
 * La bandeja de notificaciones: una lista acotada por usuario en Redis, no una
 * tabla que crece para siempre.
 *
 * @author Jason Nunez
 */
@Component
@RequiredArgsConstructor
public class NotificationStore {

    /** Todo lo que no esté aquí se empuja y se olvida: no deja rastro en la bandeja. */
    private static final Set<String> PERSISTIBLES = Set.of(
            Notification.TIPO_STOCK_BAJO, Notification.TIPO_ACCESO, Notification.TIPO_GENERAL);

    private static final int MAX_HISTORIAL = 100;
    private static final Duration TTL = Duration.ofDays(30);
    private static final String CLAVE_SECUENCIA = "notif:seq";

    private final RedisTemplate<String, Notification> redis;

    public boolean esPersistible(String tipo) {
        return PERSISTIBLES.contains(tipo);
    }

    /** Único aunque la notificación no se guarde: el cliente lo necesita igual para deduplicar. */
    public Long siguienteId() {
        return redis.opsForValue().increment(CLAVE_SECUENCIA);
    }

    public void guardar(Long userId, Notification n) {
        String clave = claveDe(userId);
        ListOperations<String, Notification> lista = redis.opsForList();

        lista.leftPush(clave, n);
        lista.trim(clave, 0, MAX_HISTORIAL - 1);
        redis.expire(clave, TTL);
    }

    public List<Notification> listar(Long userId) {
        List<Notification> lista = redis.opsForList().range(claveDe(userId), 0, MAX_HISTORIAL - 1);
        return lista != null ? lista : List.of();
    }

    public Notification buscar(Long userId, Long idNotificacion) {
        return listar(userId).stream()
                .filter(n -> idNotificacion.equals(n.getIdNotificacion()))
                .findFirst()
                .orElse(null);
    }

    public void eliminar(Long userId, Long idNotificacion) {
        reescribir(userId, actuales -> actuales.stream()
                .filter(n -> !idNotificacion.equals(n.getIdNotificacion()))
                .toList());
    }

    public void marcarLeida(Long userId, Long idNotificacion) {
        reescribir(userId, actuales -> actuales.stream()
                .peek(n -> {
                    if (idNotificacion.equals(n.getIdNotificacion())) n.setLeido(true);
                })
                .toList());
    }

    /** @return cuántas pasaron de no leída a leída */
    public int marcarTodasLeidas(Long userId) {

        List<Notification> actuales = listar(userId);
        int marcadas = 0;

        for (Notification n : actuales) {
            if (!Boolean.TRUE.equals(n.getLeido())) {
                n.setLeido(true);
                marcadas++;
            }
        }

        if (marcadas > 0) reemplazar(userId, actuales);
        return marcadas;
    }

    // -----------------------------------------------------------------------

    private void reescribir(Long userId, Function<List<Notification>, List<Notification>> transformar) {
        reemplazar(userId, transformar.apply(listar(userId)));
    }

    /**
     * Sustituye la lista entera, en el mismo orden (más reciente primero).
     *
     * <p>No es atómico frente a un push concurrente, y no hace falta serlo: son
     * acciones humanas —marcar, borrar— sobre una bandeja de a lo sumo cien
     * elementos, no un contador de alta frecuencia.
     */
    private void reemplazar(Long userId, List<Notification> nuevas) {

        String clave = claveDe(userId);
        redis.delete(clave);

        if (!nuevas.isEmpty()) {
            redis.opsForList().rightPushAll(clave, nuevas);
            redis.expire(clave, TTL);
        }
    }

    private String claveDe(Long userId) {
        return "notif:user:" + userId;
    }
}
