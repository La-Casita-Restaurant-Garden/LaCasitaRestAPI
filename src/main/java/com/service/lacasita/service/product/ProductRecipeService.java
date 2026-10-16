package com.service.lacasita.service.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.RecipeLine;
import com.service.lacasita.dto.RecipeRequest;
import com.service.lacasita.entity.ProductRecipe;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.InventoryRespository;
import com.service.lacasita.repository.ProductRecipeRepository;
import com.service.lacasita.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * La receta de un producto: cuánto de cada insumo consume una unidad.
 *
 * <h2>Por qué existe este servicio</h2>
 * <p>La tabla y el consumo automático llevaban tiempo funcionando, pero no había
 * forma de <b>cargar</b> una receta salvo escribiendo SQL contra Supabase. Era la
 * única parte del sistema que se administraba fuera de la aplicación, y eso
 * significa en la práctica que no se mantiene: nadie entra a la base a corregir un
 * gramaje.
 *
 * <h2>Se guarda entera, no línea a línea</h2>
 * <p>Se borran las que había y se escriben las que llegan. Con altas y bajas
 * sueltas, una pantalla que quedó abierta mientras otro editaba acabaría mezclando
 * dos versiones de la receta sin que nadie lo note.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class ProductRecipeService {

    private final ProductRecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final InventoryRespository inventoryRepository;

    /** La receta de un producto, con el nombre y la unidad de cada insumo. */
    @Transactional(readOnly = true)
    public List<RecipeLine> getReceta(UUID productId) {

        if (productId == null) {
            throw new BusinessException("Falta el producto", 400);
        }

        return recipeRepository.findDetalleByProductId(productId);
    }

    /**
     * Reemplaza la receta completa.
     *
     * <p>Las validaciones duplican constraints que la base ya tiene. Es a
     * propósito: una violación de clave ajena llega a la pantalla como un 500 sin
     * explicación, y quien está cargando recetas necesita saber <b>cuál</b> insumo
     * no existe o <b>qué</b> cantidad es inválida.
     */
    @Transactional
    public List<RecipeLine> guardar(RecipeRequest peticion) {

        UUID productId = peticion.getProductId();

        if (productId == null) {
            throw new BusinessException("Falta el producto", 400);
        }

        if (!productRepository.existsById(productId)) {
            throw new BusinessException("Producto no encontrado: " + productId, 404);
        }

        // Validar ANTES de borrar. Si se hiciera al revés, una línea mala —un
        // insumo que ya no existe, una cantidad en cero— dejaría al producto sin
        // la receta que sí tenía: se borra, falla al escribir y la transacción
        // revierte... salvo que alguien capture la excepción más arriba. No vale
        // la pena depender de eso para no perder datos.
        List<ProductRecipe> aGuardar = validar(productId, peticion.getLineas());

        // El flush intermedio importa: sin él, Hibernate puede ordenar los INSERT
        // antes del DELETE y chocar contra la clave primaria al reenviar la misma
        // línea.
        recipeRepository.deleteByProductId(productId);
        recipeRepository.flush();

        if (!aGuardar.isEmpty()) {
            recipeRepository.saveAll(aGuardar);
        }

        return recipeRepository.findDetalleByProductId(productId);
    }

    private List<ProductRecipe> validar(UUID productId, List<RecipeRequest.Linea> lineas) {

        List<ProductRecipe> resultado = new ArrayList<>();

        if (lineas == null || lineas.isEmpty()) {
            // Vaciar la receta es legítimo: un producto sin receta simplemente no
            // descuenta nada.
            return resultado;
        }

        // El mismo insumo dos veces reventaría contra la clave primaria; aquí se
        // avisa de cuál es en vez de devolver un 500 de integridad.
        Set<Long> vistos = new HashSet<>();

        for (RecipeRequest.Linea l : lineas) {

            if (l.getInventoryId() == null) {
                throw new BusinessException("Hay una línea sin insumo", 400);
            }

            String insumo = inventoryRepository.findById(l.getInventoryId())
                    .map(i -> i.getName())
                    .orElseThrow(() -> new BusinessException(
                            "El insumo " + l.getInventoryId() + " no existe en el inventario", 404));

            if (!vistos.add(l.getInventoryId())) {
                throw new BusinessException(
                        "El insumo '" + insumo + "' está repetido en la receta", 400);
            }

            BigDecimal cantidad = l.getQtyPerUnit();

            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "La cantidad de '" + insumo + "' tiene que ser mayor que cero", 400);
            }

            resultado.add(new ProductRecipe(productId, l.getInventoryId(), cantidad, null));
        }

        return resultado;
    }
}
