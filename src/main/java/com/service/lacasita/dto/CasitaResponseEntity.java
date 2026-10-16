package com.service.lacasita.dto;

import java.util.List;
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
public class CasitaResponseEntity<T> {
    
    private List<T> body;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
