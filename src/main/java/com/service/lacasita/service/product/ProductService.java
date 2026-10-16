package com.service.lacasita.service.product;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.service.lacasita.repository.ProductRepository;
import com.service.lacasita.repository.CategoryRepository;
import com.service.lacasita.service.s3.S3Service;
import com.service.lacasita.dto.ProductResponse;
import com.service.lacasita.entity.Category;
import com.service.lacasita.entity.Product;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.utils.CategoryTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private S3Service s3Service;

    private final DynamoDbClient dynamoDbClient;

    public ProductService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * El menú de hoy: activos y con {@code show_in_menu = true}.
     */
    @Cacheable(value = "menuProducts")
    public List<Product> getMenuProducts() {
        return productRepository.findAllInMenu();
    }

    /**
     * Get all products paginated
     */
    public Page<ProductResponse> findAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return productRepository.findAll(pageable).map(ProductService::toResponse);
    }

    /**
     * Mapea un producto a su DTO.
     *
     */
    public static ProductResponse toResponse(Product p) {

        ProductResponse r = new ProductResponse(
                p.getId(),
                p.getName(),
                p.getCategory(),
                p.getImage(),
                p.getDescription(),
                p.getPrice(),
                p.getShowInMenu(),
                p.getCreated_at());

        r.setAplicaItbms(!Boolean.FALSE.equals(p.getAplicaItbms()));
        r.setActivo(!Boolean.FALSE.equals(p.getActivo()));

        return r;
    }

    /**
     * Retira o devuelve un producto al menú.
     *
     * <p>Sustituye a {@code deleteProduct}: borrar la fila dejaba sin referencia a
     * {@code order_items}, {@code product_recipe} y {@code orden_descuento}, y con
     * ella un pedido de hace un mes deja de poder explicarse.
     */
    @CacheEvict(value = { "products", "menuProducts" }, allEntries = true)
    public Product alternarActivo(UUID id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado: " + id, 404));

        p.setActivo(Boolean.FALSE.equals(p.getActivo()));

        return productRepository.save(p);
    }

    /**
     * Prende o apaga la visibilidad en el menú del día, sin pasar por el
     * formulario completo — para cuando se necesita rápido desde la lista.
     */
    @CacheEvict(value = { "products", "menuProducts" }, allEntries = true)
    public Product alternarShowInMenu(UUID id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado: " + id, 404));

        p.setShowInMenu(Boolean.FALSE.equals(p.getShowInMenu()));

        return productRepository.save(p);
    }

    public String saveImage(MultipartFile img) {
        try {

            return s3Service.uploadFileToS3(img);
        } catch (IOException e) {
            throw new RuntimeException("Error uploading image", e);
        }
    }

    @CacheEvict(value = { "products", "menuProducts" }, allEntries = true)
    public Product createProduct(ProductResponse product, MultipartFile file) {

        Product p = new Product();

        if (file != null && !file.isEmpty()) {
            String imageUrl = saveImage(file);
            p.setImage(imageUrl);
        }

        p.setName(product.getName());
        p.setCategory(product.getCategory());
        p.setDescription(product.getDescription());
        p.setPrice(product.getPrice());
        p.setAplicaItbms(product.getAplicaItbms() == null || product.getAplicaItbms());
        p.setActivo(product.getActivo() == null || product.getActivo());
        p.setShowInMenu(product.getShowInMenu() == null || product.getShowInMenu());
        p.setCreated_at(product.getCreated_at());

        return productRepository.save(p);
    }

    @CacheEvict(value = { "products", "menuProducts" }, allEntries = true)
    public Product updateProduct(ProductResponse product, MultipartFile file) {

        Product p = productRepository.findById(product.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String imagenAnterior = p.getImage();

        if (file != null && !file.isEmpty()) {
            p.setImage(saveImage(file));
        }

        p.setName(product.getName());
        p.setCategory(product.getCategory());
        p.setDescription(product.getDescription());
        p.setPrice(product.getPrice());
        p.setAplicaItbms(product.getAplicaItbms() == null || product.getAplicaItbms());
        p.setShowInMenu(product.getShowInMenu() == null || product.getShowInMenu());

        Product guardado = productRepository.save(p);

        // Después de guardar: si algo falla antes, la imagen vieja se conserva y no
        // queda un producto sin foto.
        if (file != null && !file.isEmpty() && imagenAnterior != null
                && !imagenAnterior.equals(guardado.getImage())) {
            s3Service.deleteFileFromS3(imagenAnterior);
        }

        return guardado;
    }

    public Optional<Product> getProductById(UUID id) {
        return productRepository.findById(id);
    }

    /**
     * Las categorias del MENÚ: las de tipo PRODUCT y las BOTH.
     */
    @Cacheable(value = "categories")
    public List<Category> getCatergories() {

        return categoryRepository.findPorTipo(CategoryTypes.DE_MENU);
    }

    /** Las del ALMACEN: las de tipo INVENTORY y las BOTH. Para clasificar insumos. */
    @Cacheable(value = "categoriesInventory")
    public List<Category> getCategoriasDeInventario() {

        return categoryRepository.findPorTipo(CategoryTypes.DE_INVENTARIO);
    }

    /**
     * Las de MENÚ
     */
    @Cacheable(value = "categoriesVisibles")
    public List<Category> getCategoriasVisiblesEnPedido() {

        return categoryRepository.findPorTipo(CategoryTypes.DE_MENU).stream()
                .filter(c -> !Boolean.FALSE.equals(c.getMostrarEnPedido()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
