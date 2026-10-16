package com.service.lacasita.service.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.CompraRequest;
import com.service.lacasita.dto.CompraResumen;
import com.service.lacasita.entity.Compra;
import com.service.lacasita.entity.CompraLinea;
import com.service.lacasita.entity.Inventory;
import com.service.lacasita.entity.InventoryMovement;
import com.service.lacasita.entity.Proveedor;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.CompraRepository;
import com.service.lacasita.repository.InventoryRespository;
import com.service.lacasita.repository.ProveedorRepository;
import com.service.lacasita.service.inventory.InventoryService;
import com.service.lacasita.utils.CostoPromedio;

import lombok.RequiredArgsConstructor;

/**
 * La compra como documento.
 *
 * <p>Nace en BORRADOR y no toca el inventario. Solo al CONFIRMAR genera un
 * movimiento PURCHASE por línea, y a partir de ahí no se edita: se anula y se
 * rehace. Es la regla del libro de movimientos aplicada un nivel más arriba —una
 * factura archivada no cambia porque alguien corrija un número en pantalla.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final InventoryRespository inventoryRepository;
    private final InventoryService inventoryService;
    private final PayableService payableService;

    @Transactional(readOnly = true)
    public Page<CompraResumen> listar(int page, int size) {
        return compraRepository.findPagina(PageRequest.of(page, size)).map(CompraResumen::de);
    }

    @Transactional(readOnly = true)
    public Compra porId(Long id) {
        return compraRepository.findConLineas(id)
                .orElseThrow(() -> new BusinessException("La compra no existe", 404));
    }


    @Transactional
    public Compra guardar(CompraRequest req, Long userId) {

        if (req == null || req.getProveedorId() == null) {
            throw new BusinessException("La compra necesita un proveedor", 400);
        }

        if (req.getLineas() == null || req.getLineas().isEmpty()) {
            throw new BusinessException("La compra necesita al menos una línea", 400);
        }

        Proveedor proveedor = proveedorRepository.findById(req.getProveedorId())
                .orElseThrow(() -> new BusinessException("El proveedor no existe", 404));

        Compra compra = req.getId() == null ? new Compra() : porId(req.getId());

        if (!Compra.BORRADOR.equals(compra.getEstado())) {
            throw new BusinessException(
                    "Una compra %s no se edita: anúlala y vuelve a cargarla".formatted(
                            compra.getEstado().toLowerCase()), 409);
        }

        validarFacturaUnica(req, proveedor.getId());

        compra.setProveedor(proveedor);
        compra.setNumeroFactura(vacioANulo(req.getNumeroFactura()));
        compra.setFecha(req.getFecha() != null ? req.getFecha() : LocalDate.now());
        compra.setCondicion(Compra.CREDITO.equals(req.getCondicion()) ? Compra.CREDITO : Compra.CONTADO);
        compra.setIdpago(Compra.CREDITO.equals(compra.getCondicion()) ? null : req.getIdpago());
        compra.setNota(req.getNota());

        if (compra.getCreatedBy() == null) {
            compra.setCreatedBy(userId);
        }

        compra.getLineas().clear();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraRequest.Linea l : req.getLineas()) {
            CompraLinea linea = construirLinea(compra, l);
            compra.getLineas().add(linea);
            total = total.add(linea.getSubtotal());
        }

        compra.setTotal(total);

        return compraRepository.save(compra);
    }

    private CompraLinea construirLinea(Compra compra, CompraRequest.Linea origen) {

        if (origen.getInventoryId() == null) {
            throw new BusinessException("Hay una línea sin insumo", 400);
        }

        if (origen.getCantidad() == null || origen.getCantidad().signum() <= 0) {
            throw new BusinessException("La cantidad de cada línea tiene que ser mayor que cero", 400);
        }

        BigDecimal costo = origen.getCosto() != null ? origen.getCosto() : BigDecimal.ZERO;

        if (costo.signum() < 0) {
            throw new BusinessException("El costo no puede ser negativo", 400);
        }

        Inventory insumo = inventoryRepository.findById(origen.getInventoryId())
                .orElseThrow(() -> new BusinessException(
                        "El insumo %s no existe".formatted(origen.getInventoryId()), 404));

    
        BigDecimal factor = insumo.getPurchaseFactor() != null && insumo.getPurchaseFactor().signum() > 0
                ? insumo.getPurchaseFactor()
                : BigDecimal.ONE;

        CompraLinea linea = new CompraLinea();
        linea.setCompra(compra);
        linea.setInventoryId(insumo.getId());
        linea.setCantidad(origen.getCantidad());
        linea.setFactor(factor);
        linea.setCantidadBase(origen.getCantidad().multiply(factor));
        linea.setCosto(costo);
        linea.setCostoBase(CostoPromedio.porUnidadDeConsumo(costo, factor));
        linea.setSubtotal(origen.getCantidad().multiply(costo).setScale(2, RoundingMode.HALF_UP));

        return linea;
    }


    @Transactional
    public Compra confirmar(Long id, Long userId) {

        Compra compra = porId(id);

        if (Compra.CONFIRMADA.equals(compra.getEstado())) {
            throw new BusinessException("Esta compra ya estaba confirmada", 409);
        }

        if (Compra.ANULADA.equals(compra.getEstado())) {
            throw new BusinessException("Una compra anulada no se puede confirmar", 409);
        }

        if (compra.getLineas().isEmpty()) {
            throw new BusinessException("La compra no tiene líneas", 400);
        }

        for (CompraLinea linea : compra.getLineas()) {
            inventoryService.registerMovement(
                    linea.getInventoryId(),
                    InventoryMovement.TYPE_PURCHASE,
                    linea.getCantidadBase(),
                    linea.getCostoBase(),
                    null,
                    compra.getId(),
                    referencia(compra),
                    userId);
        }

        if (Compra.CREDITO.equals(compra.getCondicion()) && compra.getTotal().signum() > 0) {
            payableService.cargar(compra.getProveedor().getId(), compra.getTotal(),
                    compra.getId(), referencia(compra), userId);
        }

        compra.setEstado(Compra.CONFIRMADA);

        return compraRepository.save(compra);
    }

    @Transactional
    public Compra anular(Long id, String motivo, Long userId) {

        Compra compra = porId(id);

        if (Compra.ANULADA.equals(compra.getEstado())) {
            throw new BusinessException("Esta compra ya estaba anulada", 409);
        }

        String razon = "Anulación de %s%s".formatted(
                referencia(compra),
                motivo != null && !motivo.isBlank() ? ": " + motivo.trim() : "");

        if (Compra.CONFIRMADA.equals(compra.getEstado())) {

            for (CompraLinea linea : compra.getLineas()) {
                inventoryService.registerMovement(
                        linea.getInventoryId(),
                        InventoryMovement.TYPE_ADJUSTMENT,
                        linea.getCantidadBase().negate(),
                        null,
                        null,
                        compra.getId(),
                        razon,
                        userId);
            }

            if (Compra.CREDITO.equals(compra.getCondicion()) && compra.getTotal().signum() > 0) {
                payableService.ajustar(compra.getProveedor().getId(), compra.getTotal().negate(),
                        compra.getId(), razon, userId);
            }
        }

        compra.setEstado(Compra.ANULADA);

        return compraRepository.save(compra);
    }

    // -----------------------------------------------------------------------

    private void validarFacturaUnica(CompraRequest req, Long proveedorId) {

        String numero = vacioANulo(req.getNumeroFactura());

        if (numero == null) {
            return;
        }


        List<Compra> otras = new ArrayList<>(compraRepository.findPorFactura(proveedorId, numero));
        otras.removeIf(c -> c.getId().equals(req.getId()));

        if (!otras.isEmpty()) {
            throw new BusinessException(
                    "Ese proveedor ya tiene cargada la factura " + numero, 409);
        }
    }

    private String referencia(Compra compra) {
        return compra.getNumeroFactura() != null
                ? "compra " + compra.getNumeroFactura()
                : "compra #" + compra.getId();
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
