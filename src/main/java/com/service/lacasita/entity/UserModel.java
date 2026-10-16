package com.service.lacasita.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class UserModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private Long id;
    @Column(name = "u_name")
    private String name;
    @Column(name = "u_username")
    private String username;
    @Column(name = "u_lastname")
    private String lastname;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "u_password")
    private String password;
    @Column(name = "u_email")
    private String email;
    @Column(name = "u_phone")
    private String phone;
    @Column(name = "u_status")
    private String status;
    @Column(name = "u_image")
    private String image;

    @ManyToOne
    @JoinColumn(name = "u_rol", referencedColumnName = "rl_id")
    private Role roleId;

    public UserModel(String name, String username, String lastname, String password, String email, String status, Role roleId, String image) {
        this.name = name;
        this.username = username;
        this.lastname = lastname;
        this.password = password;
        this.email = email;
        this.status = status;
        this.roleId = roleId;
        this.image = image;
    }

}