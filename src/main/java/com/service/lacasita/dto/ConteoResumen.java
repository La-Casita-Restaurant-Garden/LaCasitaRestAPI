package com.service.lacasita.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.service.lacasita.entity.Conteo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConteoResumen {

    private Long      id;
    private LocalDate fecha;
    private String    estado;
    private String    nota;
    private Instant   createdAt;
    private Instant   cerradoAt;

    public static ConteoResumen de(Conteo c) {
        return new ConteoResumen(
                c.getId(),
                c.getFecha(),
                c.getEstado(),
                c.getNota(),
                c.getCreatedAt(),
                c.getCerradoAt()
        );
    }
}
