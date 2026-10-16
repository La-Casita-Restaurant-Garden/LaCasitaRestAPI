package com.service.lacasita.controllers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.service.lacasita.dto.Id_dto;
import com.service.lacasita.dto.ProductResponse;
import com.service.lacasita.entity.Product;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.RecipeLine;
import com.service.lacasita.dto.RecipeRequest;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.service.product.ProductRecipeService;
import com.service.lacasita.service.product.ProductService;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

@RestController()
@RequestMapping("/prod")
@PreAuthorize("@perm.lee(authentication, 'products')")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRecipeService recipeService;

    /**
     * Endpoint paginado para productos (como sales/find-all-sales)
     */
    @PostMapping("/find-all-products")
    public ResponseEntity<PageResponse<ProductResponse>> findAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductResponse> products = productService.findAllProducts(page, size);

        return new ResponseEntity<>(
                new PageResponse<>(products, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Catálogo completo.
     */
    @PostMapping("/all-products")
    public ResponseEntity<Response<List<ProductResponse>>> getAllProducts() {

        List<ProductResponse> response = productService.getAllProducts().stream()
                .map(ProductService::toResponse)
                .toList();

        return new ResponseEntity<>(
                new Response<>(response, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /**
     * El menú de hoy: lo que consume el POS al crear o editar una orden.
     */
    @PostMapping("/menu-products")
    public ResponseEntity<Response<List<ProductResponse>>> getMenuProducts() {

        List<ProductResponse> response = productService.getMenuProducts().stream()
                .map(ProductService::toResponse)
                .toList();

        return new ResponseEntity<>(
                new Response<>(response, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/find-product")
    public ResponseEntity<Response<ProductResponse>> finsProducts(@RequestBody Id_dto body) {
        try {

            UUID id = UUID.fromString(body.getId());
            Optional<Product> productOpt = productService.getProductById(id);

            if (productOpt.isEmpty()) {
                return new ResponseEntity<>(
                        new Response<>(null, new StatusCode(404, "Product not found")),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(
                    new Response<>(ProductService.toResponse(productOpt.get()),
                            new StatusCode(200, "Successfully")),
                    HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(
                    new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Actualiza un producto.
     */
    @PreAuthorize("@perm.puede(authentication, 'products', 'write')")
    @PostMapping(value = "/update-product", consumes = { "multipart/form-data" })
    public ResponseEntity<Response<ProductResponse>> updateProduct(
            @RequestPart("product") ProductResponse body,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Product product = productService.updateProduct(body, file);

        return new ResponseEntity<>(
                new Response<>(ProductService.toResponse(product),
                        new StatusCode(200, "Producto actualizado")),
                HttpStatus.OK);
    }

    /**
     * Da de alta un producto.
     */
    @PreAuthorize("@perm.puede(authentication, 'products', 'write')")
    @PostMapping(value = "/save-product", consumes = { "multipart/form-data" })
    public ResponseEntity<Response<ProductResponse>> saveProduct(
            @RequestPart("product") ProductResponse body,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Product product = productService.createProduct(body, file);

        return new ResponseEntity<>(
                new Response<>(ProductService.toResponse(product),
                        new StatusCode(200, "Producto creado")),
                HttpStatus.CREATED);
    }

    /**
     * Retira o devuelve un producto al menú.
     */
    @PreAuthorize("@perm.puede(authentication, 'products', 'write')")
    @PostMapping("/toggle-active")
    public ResponseEntity<Response<ProductResponse>> toggleActive(@RequestBody Id_dto body) {

        Product product = productService.alternarActivo(UUID.fromString(body.getId()));

        return new ResponseEntity<>(
                new Response<>(ProductService.toResponse(product),
                        new StatusCode(200, Boolean.FALSE.equals(product.getActivo())
                                ? "Producto retirado del menú"
                                : "Producto disponible otra vez")),
                HttpStatus.OK);
    }

    /**
     * Prende o apaga la visibilidad en el menú del día, directo desde la tabla.
     */
    @PreAuthorize("@perm.puede(authentication, 'products', 'write')")
    @PostMapping("/toggle-show-in-menu")
    public ResponseEntity<Response<ProductResponse>> toggleShowInMenu(@RequestBody Id_dto body) {

        Product product = productService.alternarShowInMenu(UUID.fromString(body.getId()));

        return new ResponseEntity<>(
                new Response<>(ProductService.toResponse(product),
                        new StatusCode(200, Boolean.FALSE.equals(product.getShowInMenu())
                                ? "Producto oculto del menú"
                                : "Producto visible en el menú")),
                HttpStatus.OK);
    }

    // ── Receta ────────────────────────────────────────────────────────────

    /**
     * La receta del producto: cuánto de cada insumo consume una unidad.
     */
    @PostMapping("/recipe")
    public ResponseEntity<Response<List<RecipeLine>>> getReceta(@RequestBody Id_dto body) {

        return new ResponseEntity<>(
                new Response<>(recipeService.getReceta(UUID.fromString(body.getId())),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Reemplaza la receta entera.
     */
    @PreAuthorize("@perm.puede(authentication, 'products', 'write')")
    @PostMapping("/recipe/save")
    public ResponseEntity<Response<List<RecipeLine>>> guardarReceta(@RequestBody RecipeRequest body) {

        return new ResponseEntity<>(
                new Response<>(recipeService.guardar(body),
                        new StatusCode(200, "Receta guardada")),
                HttpStatus.OK);
    }

}
