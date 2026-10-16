package com.service.lacasita.dto;

/** HTML/asunto sin guardar todavía, para previsualizarlos con datos de ejemplo. */
public record EmailTemplatePreviewRequest(String asunto, String html) {
}
