package com.service.lacasita.dto;

import com.service.lacasita.entity.Role;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse implements Serializable {

    private Long id;
    private String name;
    private String last_name;
    private String email;
    private String phone;
    private String address;
    private String status;
    private double sales;
    private int orders;
    private Role role;

}
