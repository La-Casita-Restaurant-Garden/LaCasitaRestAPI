package com.service.lacasita.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Setter
@Getter
public class Response <T> {
    
    private T body;
    private StatusCode status ;
    private String timestamp = LocalDateTime.now().toString();

    public Response(T body, StatusCode status) {
        this.body   = body;
        this.status = status;
    }
}
