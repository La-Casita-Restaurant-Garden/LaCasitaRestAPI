package com.service.lacasita.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.service.user.RoleService;
import com.service.lacasita.entity.Role;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController()
@RequestMapping("/role")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
    

    @Autowired
    private RoleService roleService;
    
    @PostMapping("/consultaroles")
    public ResponseEntity<Response<List<Role>>> getAllRoles() {
        try {
            List<Role> role = roleService.getAllRoles();
            return new ResponseEntity<>(new Response<>(role, new StatusCode(200, "Successfully")), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
            HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/consultarolusuario")
    public ResponseEntity<Response<Role>> getUserRoleById(@RequestBody UserId body) {
        try {
            Role role = roleService.getRoleById(body.getId());
            
            return new ResponseEntity<>(new Response<>(role, new StatusCode(200, "Successfully")), HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>(new Response<>(null, new StatusCode(500, e.getMessage())),
            HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
    }
}
