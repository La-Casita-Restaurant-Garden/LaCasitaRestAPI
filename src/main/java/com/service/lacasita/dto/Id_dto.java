package com.service.lacasita.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Id_dto {
    @Schema(description = "Unique identifier for the product", example = "204c7b1a-d4ea-4a4e-a76e-3671c2ee6086")
    private String id;
}
