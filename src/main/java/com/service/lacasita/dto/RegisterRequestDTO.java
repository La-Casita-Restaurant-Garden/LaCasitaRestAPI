package com.service.lacasita.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
  
    private String name;
    private String last_name;
    private String email;
    private String phone;
    private String password;
    
}


