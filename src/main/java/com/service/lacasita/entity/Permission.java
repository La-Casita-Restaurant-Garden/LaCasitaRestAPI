package com.service.lacasita.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Tabla de permisos por rol y módulo.
 * Ejemplo: ADMIN puede leer/escribir/borrar en 'dashboard', 'sales', etc.
 *          WAITER puede leer en 'orders', 'tables', pero no en 'sales'.
 */
@Entity
@Table(name = "permisos", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"role_id", "module_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "rl_id", nullable = false)
    private Role role;

    /** Clave del módulo (ej: 'dashboard', 'sales', 'orders', 'products', 'inventory', etc.) */
    @Column(name = "module_key", nullable = false, length = 50)
    private String moduleKey;

    /** Nombre visible del módulo */
    @Column(name = "module_label", length = 100)
    private String moduleLabel;

    @Column(name = "can_read", nullable = false)
    private Boolean canRead = false;

    @Column(name = "can_write", nullable = false)
    private Boolean canWrite = false;

    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = false;
}
