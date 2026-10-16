package com.service.lacasita.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusCode implements Serializable {

    private int code;
    private String message;

    
}
