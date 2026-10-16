package com.service.lacasita.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Setter
@Getter
public class PageResponse<T> {

    private List<T> body;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private StatusCode status;
    private String timestamp = LocalDateTime.now().toString();

    public PageResponse(Page<T> page, StatusCode status) {

        this.body           = page.getContent();
        this.page           = page.getNumber();
        this.size           = page.getSize();
        this.totalElements  = page.getTotalElements();
        this.totalPages     = page.getTotalPages();
        this.status         = status;

    }
}