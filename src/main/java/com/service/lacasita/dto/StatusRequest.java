package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un estado de orden en el cuerpo de la petición.
 *
 * <p>Un objeto y no un {@code @RequestBody String}: el endpoint viejo recibe la
 * cadena cruda y eso obliga al cliente a mandar texto plano en vez de JSON, que es
 * lo que manda el resto de la API. Con un DTO, el cuerpo es {@code {"status":"PENDING"}}
 * y encaja con el envoltorio que ya usa todo lo demás.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusRequest {

    private String status;
}
