package com.service.lacasita.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserId { 

    @Schema(example = "1234")
    private Long id; 
}