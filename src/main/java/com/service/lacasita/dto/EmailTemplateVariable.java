package com.service.lacasita.dto;

/**
 * Un parámetro disponible en una plantilla de correo — para documentarlo en
 * el editor de Settings, donde se pega el marcador {@code {{clave}}} tal cual.
 */
public record EmailTemplateVariable(String clave, String descripcion, String ejemplo) {
}
