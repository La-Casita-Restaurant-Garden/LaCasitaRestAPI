package com.service.lacasita.service.customer;

import com.service.lacasita.dto.CasitaResponseEntity;
import com.service.lacasita.dto.CustomerResponse;
import com.service.lacasita.entity.Customer;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.CustomerRepository;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.repository.SalesRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import com.service.lacasita.service.auth.AuthService;;

@Service
public class CustomerService {

    /** Un cliente recién dado de alta nace activo. */
    private static final String ESTADO_POR_DEFECTO = "ACTIVE";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private AuthService authService;

    /*
     * Method to get all customers
     */
    @Cacheable(value = "customersPage", key = "#page + '-' + #size")
    public CasitaResponseEntity<CustomerResponse> getAllCustomers(int page, int size) {
        return paginar(null, page, size);
    }

    /**
     * Por nombre, apellido, teléfono o correo. Sin caché a propósito: cachear
     * por page+size ignoraría el término y devolvería resultados de otra
     * búsqueda —el selector de clientes del POS necesita esto siempre fresco.
     */
    public CasitaResponseEntity<CustomerResponse> buscarClientes(String termino, int page, int size) {
        return paginar(termino, page, size);
    }

    private CasitaResponseEntity<CustomerResponse> paginar(String termino, int page, int size) {
        try {

            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

            Page<Customer> customers = (termino == null || termino.isBlank())
                    ? customerRepository.findAll(pageable)
                    : customerRepository.buscar(termino.trim(), pageable);

            List<CustomerResponse> mappedPage = customers.map(c -> new CustomerResponse(
                    c.getCustomer_id(),
                    c.getName(),
                    c.getLast_name(),
                    c.getEmail(),
                    c.getPhone(),
                    c.getAddress(),
                    c.getStatus(),
                    0,
                    0,
                    c.getRole()

            )).toList();

            return new CasitaResponseEntity<CustomerResponse>(mappedPage, customers.getNumber(), customers.getSize(),
                    customers.getTotalElements(), customers.getTotalPages());

        } catch (Exception e) {
            throw new RuntimeException("Error fetching customers: " + e.getMessage());
        }
    }

    /*
     * Method to update customer
     */
    @Caching(evict = {
            @CacheEvict(value = "customerDetail", key = "#body.customer_id"),
            @CacheEvict(value = "customersPage", allEntries = true) })
    public Customer updateCustomer(Customer body) {

        return customerRepository.findById(body.getCustomer_id()).map(existingCustomer -> {

            existingCustomer.setName(body.getName());
            existingCustomer.setLast_name(body.getLast_name());
            existingCustomer.setEmail(normalizar(body.getEmail()));
            existingCustomer.setPhone(body.getPhone());
            existingCustomer.setAddress(body.getAddress());
            existingCustomer.setStatus(body.getStatus());

            return customerRepository.save(existingCustomer);

        }).orElse(null);
    }

    
    @CacheEvict(value = "customersPage", allEntries = true)
    public CustomerResponse createCustomer(CustomerResponse body) {

        if (body.getName() == null || body.getName().isBlank()) {
            throw new BusinessException("El cliente necesita al menos un nombre", 400);
        }

        String email = normalizar(body.getEmail());

        if (email != null && customerRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Ya hay un cliente con el correo " + email, 409);
        }

        Customer newCustomer = new Customer();
        newCustomer.setName(body.getName().trim());
        newCustomer.setLast_name(normalizar(body.getLast_name()));
        newCustomer.setEmail(email);
        newCustomer.setPhone(normalizar(body.getPhone()));
        newCustomer.setAddress(normalizar(body.getAddress()));
        newCustomer.setStatus(body.getStatus() != null ? body.getStatus() : ESTADO_POR_DEFECTO);
        newCustomer.setRole(body.getRole());

        newCustomer.setPassword(authService.decodePassword(authService.generateRandomPassword(8)));

        Customer guardado = customerRepository.save(newCustomer);

        body.setId(guardado.getCustomer_id());
        body.setEmail(guardado.getEmail());
        body.setStatus(guardado.getStatus());

        return body;
    }

    /** Cadena vacía y solo espacios valen lo mismo que ausente: null. */
    private String normalizar(String valor) {
        if (valor == null || valor == "") return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /*
     * Method to get customer by id
     */
    public Optional<Customer> getCustomerById(Long id) {

        return customerRepository.findById(id);
    }

    /*
     * Method to get customer and details od orders and sale
     */
    @Cacheable(value = "customerDetail", key = "#id")
    public CustomerResponse getDetailCustomer(Long id) {

        int totalOrders = 0;
        double totalSales = 0;

        Optional<Customer> customer = customerRepository.findById(id);
        CustomerResponse customerResponse = new CustomerResponse();

        totalOrders = orderRepository.totalOrdersByCustomer(id);
        totalSales = salesRepository.totalSalesByCustomer(id);

        customerResponse.setId(customer.get().getCustomer_id());
        customerResponse.setName(customer.get().getName());
        customerResponse.setLast_name(customer.get().getLast_name());
        customerResponse.setEmail(customer.get().getEmail());
        customerResponse.setPhone(customer.get().getPhone());
        customerResponse.setAddress(customer.get().getAddress());
        customerResponse.setStatus(customer.get().getStatus());
        customerResponse.setSales(totalSales);
        customerResponse.setOrders(totalOrders);
        customerResponse.setRole(customer.get().getRole());

        return customerResponse;
    }

    public Long getTotalCustomers() {
        return customerRepository.count();
    }

}
