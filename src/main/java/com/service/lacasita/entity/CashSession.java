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

/**
 * Turno de caja.
 * @author Jason Nunez
 */
@Entity
@Table(name = "caja_sesion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashSession {

    public static final String ABIERTA = "ABIERTA";
    public static final String CERRADA = "CERRADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "abierta_at", nullable = false)
    private Instant abiertaAt;

    @Column(name = "cerrada_at")
    private Instant cerradaAt;

    /** Con lo que se arranca el turno; sin él no hay contra qué cuadrar. */
    @Column(name = "fondo_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal fondoInicial;

    @Column(name = "efectivo_esperado", precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    /** contado − esperado. Negativa es faltante. */
    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "abierta_por")
    private Long abiertaPor;

    @Column(name = "cerrada_por")
    private Long cerradaPor;

    @Column(length = 500)
    private String notas;

    public boolean estaAbierta() {
        return ABIERTA.equalsIgnoreCase(estado);
    }
}
