package com.service.lacasita.service.purchase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.entity.Proveedor;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.ProveedorRepository;

import lombok.RequiredArgsConstructor;

/**
 * Proveedores.
 *
 * <p>Un proveedor no se borra nunca: sus compras y sus insumos lo referencian, y
 * sin él una factura de hace tres meses deja de poder explicarse. Se desactiva.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<Proveedor> listar() {
        return proveedorRepository.findAll(
                org.springframework.data.domain.Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public List<Proveedor> activos() {
        return proveedorRepository.findActivos();
    }

    @Transactional(readOnly = true)
    public Proveedor porId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("El proveedor no existe", 404));
    }

    @Transactional
    public Proveedor guardar(Proveedor entrada) {

        String nombre = entrada == null || entrada.getNombre() == null ? "" : entrada.getNombre().trim();

        if (nombre.isEmpty()) {
            throw new BusinessException("El proveedor necesita un nombre", 400);
        }

        // Mismo criterio que las categorías: comparando exacto, 'Distribuidora X'
        // y 'distribuidora x ' acaban siendo dos, y las compras se reparten entre
        // los dos sin que nadie lo note.
        for (Proveedor otro : proveedorRepository.findPorNombre(nombre)) {
            if (!otro.getId().equals(entrada.getId())) {
                throw new BusinessException("Ya existe un proveedor llamado " + otro.getNombre(), 409);
            }
        }

        Proveedor proveedor = entrada.getId() == null ? new Proveedor() : porId(entrada.getId());

        proveedor.setNombre(nombre);
        proveedor.setContacto(entrada.getContacto());
        proveedor.setTelefono(entrada.getTelefono());
        proveedor.setEmail(entrada.getEmail());
        proveedor.setNota(entrada.getNota());

        if (entrada.getActivo() != null) {
            proveedor.setActivo(entrada.getActivo());
        }

        return proveedorRepository.save(proveedor);
    }

    /**
     * Baja lógica. Deja de ofrecerse al cargar una compra, pero el histórico sigue
     * pudiendo nombrarlo.
     */
    @Transactional
    public Proveedor alternarActivo(Long id) {

        Proveedor proveedor = porId(id);
        proveedor.setActivo(!Boolean.TRUE.equals(proveedor.getActivo()));

        return proveedorRepository.save(proveedor);
    }
}
