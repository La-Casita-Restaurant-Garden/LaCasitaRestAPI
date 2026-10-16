package com.service.lacasita.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Tipo de descuento que el negocio puede aplicar.
 *
 * <h2>Alcance: la distinción que importa</h2>
 * <p>Un descuento de {@link #ALCANCE_PRODUCTO} rebaja una línea concreta —una
 * promoción, el precio de empleado—. Uno de {@link #ALCANCE_ORDEN} va sobre el
 * consumo completo de una persona.
 *
 * <p><b>El de jubilado es de orden</b>, no de producto: es un derecho de quien
 * consume, no una rebaja de un plato. Modelarlo como descuento de producto cuadra
 * por casualidad hasta que el cliente pide algo que no lo lleva.
 *
 * <h2>Porcentaje, monto fijo o libre</h2>
 * <p>Como mucho uno de los dos. Si ninguno está definido, el importe lo escribe
 * quien aplica el descuento: es el caso de la cortesía, que nunca es una cifra fija.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "tipo_descuento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountType {

    /** Sobre el consumo completo: jubilado, cortesía. */
    public static final String ALCANCE_ORDEN = "ORDEN";

    /** Sobre una línea: promoción, precio de empleado. */
    public static final String ALCANCE_PRODUCTO = "PRODUCTO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String alcance;

    /** Excluyente con {@link #montoFijo}. */
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "monto_fijo", precision = 12, scale = 2)
    private BigDecimal montoFijo;

    @Column(name = "requiere_motivo", nullable = false)
    private Boolean requiereMotivo;

    /** Conceder rebajas grandes no debe estar al alcance de cualquiera. */
    @Column(name = "requiere_admin", nullable = false)
    private Boolean requiereAdmin;

    /** Cédula del beneficiario; el descuento de jubilado hay que poder justificarlo. */
    @Column(name = "requiere_documento", nullable = false)
    private Boolean requiereDocumento;

    @Column(nullable = false)
    private Boolean activo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /** Sin porcentaje ni monto fijo, el importe lo decide quien lo aplica. */
    public boolean esImporteLibre() {
        return porcentaje == null && montoFijo == null;
    }

    public boolean esDeOrden() {
        return ALCANCE_ORDEN.equalsIgnoreCase(alcance);
    }
}
