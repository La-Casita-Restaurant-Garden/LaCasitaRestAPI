package com.service.lacasita.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UpdateTimestamp;

/**
 * Parámetro del negocio, editable sin redesplegar.
 * @author Jason Nunez
 */
@Entity
@Table(name = "configuracion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuracion {

    /** Porcentaje de ITBMS. */
    public static final String ITBMS_TASA = "itbms_tasa";

    /** Interruptor general: en false no se cobra impuesto en ningún pedido. */
    public static final String ITBMS_ACTIVO = "itbms_activo";
    public static final String NEGOCIO_NOMBRE = "negocio_nombre";
    public static final String NEGOCIO_SLOGAN = "negocio_slogan";
    public static final String NEGOCIO_TELEFONO = "negocio_telefono";
    public static final String NEGOCIO_EMAIL = "negocio_email";
    public static final String NEGOCIO_DIRECCION = "negocio_direccion";
    public static final String NEGOCIO_MONEDA = "negocio_moneda";
    public static final String NEGOCIO_ZONA = "negocio_zona";
    public static final String NEGOCIO_LOGO = "negocio_logo";
    public static final String NEGOCIO_IMAGEN_COTIZACION = "negocio_imagen_cotizacion";
    public static final String ORDEN_CLIENTE_DEFECTO_ID = "orden_cliente_defecto_id";
    public static final String ORDEN_CATEGORIA_DEFECTO_ID = "orden_categoria_defecto_id";

    @Id
    @Column(length = 60)
    private String clave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(length = 255)
    private String descripcion;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
