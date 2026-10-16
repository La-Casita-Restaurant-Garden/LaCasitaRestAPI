package com.service.lacasita.service.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.CategoryRequest;
import com.service.lacasita.dto.CategoryView;
import com.service.lacasita.entity.Category;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.CategoryRepository;
import com.service.lacasita.utils.CategoryTypes;

/**
 * Administración del catálogo de categorías.
 * @author Jason Nunez
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /** Todas, con el recuento de quién las usa. */
    public List<CategoryView> listar() {

        List<CategoryView> vistas = new ArrayList<>();

        for (Object[] fila : categoryRepository.findConUso()) {

            Category c = (Category) fila[0];

            vistas.add(new CategoryView(
                    c.getId(),
                    c.getName(),
                    CategoryTypes.normalizar(c.getType()),
                    !Boolean.FALSE.equals(c.getMostrarEnPedido()),
                    ((Number) fila[1]).longValue(),
                    ((Number) fila[2]).longValue()));
        }

        return vistas;
    }

    /**
     * Alta o edición. {@code id} nulo crea.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "categoriesInventory", allEntries = true),
            @CacheEvict(value = "categoriesVisibles", allEntries = true)
    })
    public CategoryView guardar(CategoryRequest req) {

        String nombre = req == null || req.getName() == null ? "" : req.getName().trim();

        if (nombre.isEmpty()) {
            throw new BusinessException("La categoría necesita un nombre");
        }

        String tipo = CategoryTypes.normalizar(req.getType());

        for (Category otra : categoryRepository.findPorNombre(nombre)) {
            if (!otra.getId().equals(req.getId())) {
                throw new BusinessException("Ya existe una categoría llamada " + otra.getName());
            }
        }

        Category categoria;

        if (req.getId() == null) {
            categoria = new Category();
        } else {
            categoria = categoryRepository.findById(req.getId())
                    .orElseThrow(() -> new BusinessException("La categoría no existe", 404));

            validarCambioDeTipo(categoria, tipo);
        }

        categoria.setName(nombre);
        categoria.setType(tipo);
        if (req.getMostrarEnPedido() != null) categoria.setMostrarEnPedido(req.getMostrarEnPedido());

        Category guardada = categoryRepository.save(categoria);

        long[] uso = usoDe(guardada.getId());

        return new CategoryView(guardada.getId(), guardada.getName(), guardada.getType(),
                !Boolean.FALSE.equals(guardada.getMostrarEnPedido()), uso[0], uso[1]);
    }

    /**
     * Baja. Solo si no la usa nadie.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "categoriesInventory", allEntries = true),
            @CacheEvict(value = "categoriesVisibles", allEntries = true)
    })
    public void eliminar(Long id) {

        if (id == null) {
            throw new BusinessException("Falta la categoría a eliminar");
        }

        Category categoria = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("La categoría no existe", 404));

        long[] uso = usoDe(id);

        if (uso[0] > 0 || uso[1] > 0) {
            throw new BusinessException(
                    "No se puede eliminar: " + descripcionDeUso(uso)
                            + ". Cámbiales la categoría primero.");
        }

        categoryRepository.delete(categoria);
    }

    /**
     * Un cambio de tipo no puede dejar fuera a lo que ya la usa.
     */
    private void validarCambioDeTipo(Category actual, String nuevoTipo) {

        long[] uso = usoDe(actual.getId());

        boolean seguiraEnMenu = CategoryTypes.DE_MENU.contains(nuevoTipo);
        boolean seguiraEnInventario = CategoryTypes.DE_INVENTARIO.contains(nuevoTipo);

        if (uso[0] > 0 && !seguiraEnMenu) {
            throw new BusinessException(
                    "No puede dejar de ser de menú: " + uso[0] + " producto(s) la usan");
        }

        if (uso[1] > 0 && !seguiraEnInventario) {
            throw new BusinessException(
                    "No puede dejar de ser de inventario: " + uso[1] + " insumo(s) la usan");
        }
    }

    /** {productos, insumos} de una categoría. */
    private long[] usoDe(Long id) {

        if (id == null) {
            return new long[] { 0, 0 };
        }

        return new long[] {
                categoryRepository.contarProductos(id),
                categoryRepository.contarInsumos(id)
        };
    }

    private String descripcionDeUso(long[] uso) {

        if (uso[0] > 0 && uso[1] > 0) {
            return uso[0] + " producto(s) y " + uso[1] + " insumo(s) la usan";
        }

        return uso[0] > 0
                ? uso[0] + " producto(s) la usan"
                : uso[1] + " insumo(s) la usan";
    }
}
