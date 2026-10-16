package com.service.lacasita.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.service.lacasita.service.orders.OrderService;
import com.service.lacasita.dto.OrderCustomerResp;
import com.service.lacasita.dto.OrderSummary;
import com.service.lacasita.dto.KitchenResponse;
import com.service.lacasita.dto.OrderResponse;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.StatusRequest;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.OrderItem;
import com.service.lacasita.entity.PaymentMethod;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.event.OrderCreatedEvent;
import com.service.lacasita.repository.UserRespository;

import java.util.List;

@RestController()
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management operations")
@PreAuthorize("@perm.lee(authentication, 'orders')")
public class OrdersController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRespository userRepository;


    /* 
    * Method to get order
    * @author Jason Nunez
    * @version: 1.1
    * @param dto UserId
    * @return Response
    * @throws Exception
     */
    @PostMapping("/consultaorder")
    public PageResponse<OrderResponse> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<Order> orders = orderService.getAllOrders(page, size);

        Page<OrderResponse> response = orders.map(order -> {
            OrderResponse r = new OrderResponse(
                    order.getOrderId(),
                    order.getOrder_number(),
                    order.getCustomer().getCustomer_id(),
                    order.getCustomer() != null ? order.getCustomer().getName() : null,
                    order.getCustomer() != null ? order.getCustomer().getLast_name() : null,
                    order.getCustomer() != null ? order.getCustomer().getEmail() : null,
                    order.getCustomer() != null ? order.getCustomer().getPhone() : null,
                    order.getCustomer() != null ? order.getCustomer().getAddress() : null,
                    order.getStatus(),
                    order.getComments(),
                    order.getTotal(),
                    order.getDate_placed(),
                    order.getUpdated_at(),
                    order.getCreated_at(),
                    order.getPaymentMethod(),
                    null,
                    order.getIdempotencyKey());
            r.setCreatedByName(nombreDe(order.getCreatedBy()));
            return r;
        });

        return new PageResponse<>(response, new StatusCode(200, "Successfully"));
    }

    /*
    * Method to get order
    * @author Jason Nunez
    * @version: 1.1
    * @param dto UserId
    * @return Response
    * @throws Exception
     */
    @PostMapping("/consultaorderitemporid")
    public ResponseEntity<Response<List<OrderItem>>> getAllOrdersItemsById(@RequestBody UserId id) {
        try {
            List<OrderItem> orders = orderService.getAllOrderItemsById(id.getId());
            return new ResponseEntity<>(new Response<>(orders, new StatusCode(200, "Successfully")), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
    * Method to create order
    * @author Jason Nunez
    * @param dto OrderResponse
    * @return Response
    * @throws Exception
     */

    @PreAuthorize("@perm.puede(authentication, 'orders', 'write')")
    @PostMapping("/create")
    public ResponseEntity<Response<Order>> createOrder(@RequestBody OrderResponse order, Authentication auth) {

        Order createdOrder = orderService.createOrder(order, OrderCreatedEvent.ORIGIN_MANAGER, userId(auth));
        return new ResponseEntity<>(new Response<>(createdOrder, new StatusCode(200, "Successfully")),
                HttpStatus.CREATED);
    }

    private Long userId(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).map(UserModel::getId).orElse(null);
    }

    private String nombreDe(UserModel u) {
        if (u == null) return null;
        return u.getLastname() == null || u.getLastname().isBlank()
                ? u.getName() : u.getName() + " " + u.getLastname();
    }

    /*
    * Method to update order
    * @author Jason Nunez
    * @version: 1.1
    * @param dto OrderResponse
    * @return Response
    * @throws Exception
     */
    @PreAuthorize("@perm.puede(authentication, 'orders', 'write')")
    @PostMapping("/update-order")
    public ResponseEntity<Response<Order>> updateOrder(@RequestBody OrderResponse order) {
        Order updatedOrder = orderService.updateOrder(order);
        return new ResponseEntity<>(new Response<>(updatedOrder, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /*
    * Method to get order
    * @author Jason Nunez
    * @version: 1.1
    * @param dto UserId
    * @return Response
    * @throws Exception
     */
    @PostMapping("/encontrarorder")
    public ResponseEntity<Response<OrderResponse>> test(@RequestBody UserId body) {
        try {

            Long id = body.getId();
            OrderResponse orderResponse = orderService.OrderResponseMap(id);

            return new ResponseEntity<>(new Response<>(orderResponse, new StatusCode(200, "Successfully")),
                    HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/today")
    @PreAuthorize("@perm.lee(authentication, 'orders')")
    public ResponseEntity<Response<List<OrderSummary>>> ordenesDelDia() {
        return new ResponseEntity<>(
                new Response<>(orderService.getOrdenesDelDia(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Comandas de hoy en un estado, con sus líneas. La pantalla de cocina.
     */
    @PostMapping("/kitchen/today")
    @PreAuthorize("@perm.lee(authentication, 'kitchen')")
    public ResponseEntity<Response<List<KitchenResponse>>> comandasDelDia(@RequestBody StatusRequest body) {
        return new ResponseEntity<>(
                new Response<>(orderService.getComandasDelDia(body.getStatus()),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/get-payment-method")
    public ResponseEntity<Response<List<PaymentMethod>>> getPaymentMethod(@RequestBody UserId body) {
        try {

            List<PaymentMethod> paymentMethod = orderService.getPaymentMethod();
            return new ResponseEntity<>(new Response<>(paymentMethod, new StatusCode(200, "Successfully")),
                    HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
    * Method to get customer orders
    * @author Jason Nunez
    * @version: 1.1
    * @param body(id), page, size
    * @return  PageResponse
     */
    @PostMapping("/get-customer-orders")
    public ResponseEntity<PageResponse<OrderCustomerResp>> getCustomerOrders(
            @RequestBody UserId body,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            Page<OrderCustomerResp> customerOrders = orderService.getAllOrdersByCustomerId(body.getId(), page, size);

            return new ResponseEntity<>(new PageResponse<>(customerOrders, new StatusCode(200, "Successfully")),
                    HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new PageResponse<>(null, new StatusCode(500, e.getMessage())),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
    * Method to save ordesr by status KitchenResponse
    * @author Jason Nunez
    * @version: 1.1
    * @param body(id), page, size
    * @return  PageResponse KitchenResponse
     */
    @PostMapping("/updatekitchenorderstatus")
    public ResponseEntity<Response<KitchenResponse>> updateKitchenOrderStatusById(@RequestBody KitchenResponse body) {
        KitchenResponse customerOrders = orderService.updateKitchenOrderStatusById(body);

        return new ResponseEntity<>(new Response<>(customerOrders, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

}
