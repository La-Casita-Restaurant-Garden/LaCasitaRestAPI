package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConteoRequest {

    private Long        id;
    private String      nota;
    private List<Linea> lineas = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Linea {

        private Long        inventoryId;
        private BigDecimal  contado;
    }
}
