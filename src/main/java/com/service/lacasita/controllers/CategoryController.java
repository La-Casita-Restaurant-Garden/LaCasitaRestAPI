package com.service.lacasita.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.CategoryRequest;
import com.service.lacasita.dto.CategoryView;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.Category;
import com.service.lacasita.service.product.CategoryService;
import com.service.lacasita.service.product.ProductService;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @PostMapping("/menu")
    public ResponseEntity<Response<List<Category>>> delMenu() {

        return new ResponseEntity<>(
                new Response<>(productService.getCatergories(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Las del MENÚ que hoy se ofrecen en el POS. La usa crear/editar orden. */
    @PostMapping("/menu-visible")
    public ResponseEntity<Response<List<Category>>> delMenuVisible() {

        return new ResponseEntity<>(
                new Response<>(productService.getCategoriasVisiblesEnPedido(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Las del ALMACÉN: para clasificar un insumo. */
    @PostMapping("/inventario")
    public ResponseEntity<Response<List<Category>>> delInventario() {

        return new ResponseEntity<>(
                new Response<>(productService.getCategoriasDeInventario(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Todas, con el recuento de quién usa cada una. */
    @PostMapping("/list")
    @PreAuthorize("@perm.lee(authentication, 'settings')")
    public ResponseEntity<Response<List<CategoryView>>> listar() {

        return new ResponseEntity<>(
                new Response<>(categoryService.listar(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/save")
    @PreAuthorize("@perm.puede(authentication, 'settings', 'write')")
    public ResponseEntity<Response<CategoryView>> guardar(@RequestBody CategoryRequest body) {

        return new ResponseEntity<>(
                new Response<>(categoryService.guardar(body), new StatusCode(200, "Categoría guardada")),
                HttpStatus.OK);
    }

    @PostMapping("/delete")
    @PreAuthorize("@perm.puede(authentication, 'settings', 'delete')")
    public ResponseEntity<Response<Void>> eliminar(@RequestBody CategoryRequest body) {

        categoryService.eliminar(body.getId());

        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(200, "Categoría eliminada")),
                HttpStatus.OK);
    }
}
