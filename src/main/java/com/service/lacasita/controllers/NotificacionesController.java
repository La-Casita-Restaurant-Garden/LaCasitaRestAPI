package com.service.lacasita.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.service.lacasita.entity.Notification;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.notifications.NotificationService;
import com.service.lacasita.service.notifications.PushSubscriptionService;
import com.service.lacasita.service.notifications.WebPushService;
import com.service.lacasita.dto.PushSubscriptionRequest;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "API para manejar notificaciones")
@PreAuthorize("isAuthenticated()")
public class NotificacionesController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebPushService webPushService;

    @Autowired
    private PushSubscriptionService pushSubscriptionService;

    @Autowired
    private UserRespository userRepository;

    @PostMapping("/admin/requestaccess")
    public ResponseEntity<Response<Notification>> requestAccess(@RequestBody Notification body) {
        body.setTipo(Notification.TIPO_ACCESO);
        notificationService.sendToUsersAdmin(body);
        return new ResponseEntity<>(new Response<>(null, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }


    @PostMapping("user/generate")
    public ResponseEntity<Response<Notification>> createNotification(@RequestBody Notification body,
            Authentication auth) {
        // La notificación se crea siempre para quien la pide: aceptar el idUsuario
        // del body permitía inyectar notificaciones en la bandeja de otro usuario.
        body.setIdUsuario(currentUser(auth).getId());
        Notification notification = notificationService.createNotification(body);
        return new ResponseEntity<>(new Response<>(notification, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /**
     * Historial del usuario autenticado.
     *
     * Ya no recibe el id por body: se toma del token. Antes, mandar cualquier id
     * devolvía la bandeja de ese usuario.
     */
    @PostMapping("/user/find")
    public List<Notification> findByUserIdNotificacion(Authentication auth) {
        return notificationService.getMyNotification(currentUser(auth).getId());
    }

    @PostMapping("/user/delete")
    public Notification deleteNotificationByIdUser(@RequestBody Notification body, Authentication auth) {
        Notification n = requireOwned(body, auth);
        notificationService.deleteNotification(n.getIdUsuario(), n.getIdNotificacion());
        return n;
    }

    @PostMapping("/user/markasread")
    public Notification markasread(@RequestBody Notification body, Authentication auth) {
        Notification n = requireOwned(body, auth);
        notificationService.markAsRead(n.getIdUsuario(), n.getIdNotificacion());
        n.setLeido(true);
        return n;
    }

    /**
     * Marca leída toda la bandeja del usuario autenticado.
     *
     * <p>Una llamada en vez de una por notificación. El id sale del token, así que
     * no hay forma de vaciar la bandeja de otro.
     */
    @PostMapping("/user/markallasread")
    public ResponseEntity<Response<Integer>> markAllAsRead(Authentication auth) {

        int marcadas = notificationService.marcarTodasLeidas(currentUser(auth).getId());

        return new ResponseEntity<>(
                new Response<>(marcadas, new StatusCode(200, "Notificaciones marcadas")),
                HttpStatus.OK);
    }


    /** Clave pública VAPID, para que el navegador arme la suscripción. */
    @PostMapping("/push/public-key")
    public ResponseEntity<Response<String>> publicKey() {
        return new ResponseEntity<>(
                new Response<>(webPushService.publicKey(), new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /**
     * Registra o reasigna una suscripción.
     */
    @PostMapping("/push/subscribe")
    public ResponseEntity<Response<Void>> subscribe(
            @RequestBody PushSubscriptionRequest body, Authentication auth) {

        if (body == null || body.getEndpoint() == null || body.getKeys() == null
                || body.getKeys().getP256dh() == null || body.getKeys().getAuth() == null) {
            throw new BusinessException("Suscripción incompleta", 400);
        }

        pushSubscriptionService.suscribir(currentUser(auth).getId(), body.getEndpoint(),
                body.getKeys().getP256dh(), body.getKeys().getAuth());

        return new ResponseEntity<>(new Response<>(null, new StatusCode(200, "Suscrito")), HttpStatus.OK);
    }

    @PostMapping("/push/unsubscribe")
    public ResponseEntity<Response<Void>> unsubscribe(@RequestBody PushSubscriptionRequest body) {

        if (body != null && body.getEndpoint() != null) {
            pushSubscriptionService.desuscribir(body.getEndpoint());
        }

        return new ResponseEntity<>(new Response<>(null, new StatusCode(200, "Baja registrada")), HttpStatus.OK);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private UserModel currentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", 401));
    }

    private Notification requireOwned(Notification body, Authentication auth) {
        if (body == null || body.getIdNotificacion() == null) {
            throw new BusinessException("idNotificacion es obligatorio", 400);
        }

        return notificationService.encontrarDeUsuario(currentUser(auth).getId(), body.getIdNotificacion());
    }
}
