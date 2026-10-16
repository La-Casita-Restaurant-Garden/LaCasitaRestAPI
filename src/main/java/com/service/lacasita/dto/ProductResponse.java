package com.service.lacasita.dto;
import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import com.service.lacasita.entity.Category;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {

    private UUID     id;
    private String   name;
    private Category category;
    private String   image;
    private String   description;
    private Double   price;
    private Boolean  aplicaItbms;
    private Boolean  activo;
    private Boolean  showInMenu;
    private Date     created_at;

    public ProductResponse(UUID id, String name, Category category, String image, String description, Double price,Boolean showInMenu, Date created_at) {

        this.id           = id;
        this.name         = name;
        this.category     = category;
        this.image        = image;
        this.description  = description;
        this.price        = price;
        this.showInMenu   = showInMenu;
        this.created_at   = created_at;
    }
}

