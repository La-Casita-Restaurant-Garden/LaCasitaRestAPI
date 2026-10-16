package com.service.lacasita.controllers;

import com.service.lacasita.dto.CasitaResponseEntity;
import com.service.lacasita.dto.CustomerResponse;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.Customer;
import com.service.lacasita.service.customer.CustomerService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/customer")
@PreAuthorize("@perm.lee(authentication, 'clients')")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/findallcustomers")
    public CasitaResponseEntity<CustomerResponse> getUserProfiles2(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String search) {

        return (search == null || search.isBlank())
                ? customerService.getAllCustomers(page, size)
                : customerService.buscarClientes(search, page, size);
    }

    @PostMapping("/findcustomerbyid")
    public ResponseEntity<Response<Customer>> getUserProfileByid(@RequestBody UserId body) {
        try {

            Optional<Customer> customer = customerService.getCustomerById(body.getId());

            if (customer.isEmpty()) {
                return new ResponseEntity<>(new Response<>(null, new StatusCode(404, "Customer not found")),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new Response<>(customer.get(), new StatusCode(200, "Successfully")),
                    HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /*
     * Method to get customer by id
     */
    @PostMapping("/get-details-customer")
    public ResponseEntity<Response<CustomerResponse>> getDetailsCustomer(@RequestBody UserId body) {
        try {

            CustomerResponse customer = customerService.getDetailCustomer(body.getId());

            return new ResponseEntity<>(new Response<>(customer, new StatusCode(200, "Successfully")), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /*
     * Method to save customer
     */
    @PreAuthorize("@perm.puede(authentication, 'clients', 'write')")
    @PostMapping("/createcustomer")
    public ResponseEntity<Response<CustomerResponse>> saveCustomer(@RequestBody CustomerResponse body) {
        try {
            
            CustomerResponse customer = customerService.createCustomer(body);

            return new ResponseEntity<>(new Response<>(customer, new StatusCode(200, "Successfully")), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    /*
     * Method to update customer
     */
    @PreAuthorize("@perm.puede(authentication, 'clients', 'write')")
    @PostMapping("/updatecustomer")
    public ResponseEntity<Response<Customer>> updateCustomer(@RequestBody Customer body) {
        try {

            Customer customer = customerService.updateCustomer(body);

            if (customer == null) {
                return new ResponseEntity<>(new Response<>(null, new StatusCode(404, "Customer not found")),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new Response<>(customer, new StatusCode(200, "Successfully")), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
