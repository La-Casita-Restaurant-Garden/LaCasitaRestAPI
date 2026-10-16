package com.service.lacasita.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una factura de proveedor.
 */
@Entity
@Table(name = "compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Compra {

    public static final String BORRADOR = "BORRADOR";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String ANULADA = "ANULADA";

    public static final String CONTADO = "CONTADO";
    public static final String CREDITO = "CREDITO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "numero_factura", length = 60)
    private String numeroFactura;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    @Column(nullable = false, length = 12)
    private String estado = BORRADOR;

    @Column(nullable = false, length = 10)
    private String condicion = CONTADO;

    /** Mantenido, no sumado al vuelo: la lista se ordena por importe. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Con qué se pagó, cuando fue de contado. */
    @Column(name = "idpago")
    private Long idpago;

    @Column(length = 255)
    private String nota;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraLinea> lineas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
