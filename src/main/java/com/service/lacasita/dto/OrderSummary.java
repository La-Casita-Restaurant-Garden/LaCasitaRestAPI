package com.service.lacasita.dto;

import java.time.LocalDateTime;

import com.service.lacasita.utils.PaymentMethods;

/**
 * Una orden vista desde la lista del día: lo justo para reconocerla y decidir.
 *
 * <p>Se construye con una proyección JPQL, no cargando la entidad. La lista se
 * abre muchas veces por turno y traerse cada orden con sus líneas y su cliente
 * sería un N+1 sobre la consulta más frecuente del mostrador. El número de líneas
 * llega como {@code size(o.items)}, que Hibernate resuelve con una subconsulta.
 *
 * <p>{@code cobrada} se deriva del método de pago: una orden sin método es una
 * orden que todavía nadie ha pagado, que es exactamente lo que hay que ver de un
 * vistazo al volver a ella.
 *
 * @author Jason Nunez
 */
public record OrderSummary(

        Long orderId,
        String orderNumber,
        String status,
        Double total,
        LocalDateTime fecha,

        String clienteNombre,
        String clienteApellido,

        String metodoPago,
        Integer lineas) {

    /**
     * Sin método de pago no se ha cobrado.
     *
     * <p>Y "NO PAYMENT METHOD" es no tener método: es la fila de relleno que el
     * checkout asigna cuando se toma la comanda sin saber aún cómo va a pagar el
     * cliente. Comprobar solo que el nombre no venga en blanco daba por cobradas
     * justo las órdenes que el filtro "Por cobrar" tiene que sacar a flote.
     */
    public boolean isCobrada() {
        return !PaymentMethods.sinCobrar(metodoPago);
    }
}
