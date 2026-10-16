package com.service.lacasita.dto;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
}