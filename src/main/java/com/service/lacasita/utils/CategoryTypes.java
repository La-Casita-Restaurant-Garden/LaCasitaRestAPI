package com.service.lacasita.utils;
import java.util.List;

public final class CategoryTypes {

    public static final String PRODUCT = "PRODUCT";
    public static final String INVENTORY = "INVENTORY";
    public static final String BOTH = "BOTH";
    public static final List<String> DE_MENU = List.of(PRODUCT, BOTH);
    public static final List<String> DE_INVENTARIO = List.of(INVENTORY, BOTH);

    private CategoryTypes() {
    }

    public static String normalizar(String tipo) {

        if (tipo == null) {
            return PRODUCT;
        }

        String t = tipo.trim().toUpperCase();

        return switch (t) {
            case INVENTORY, BOTH, PRODUCT -> t;
            default -> PRODUCT;
        };
    }
}
