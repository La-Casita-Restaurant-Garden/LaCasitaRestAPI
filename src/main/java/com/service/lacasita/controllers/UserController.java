package com.service.lacasita.controllers;

import com.service.lacasita.dto.ChangePasswordRequest;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.user.UserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController()
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRespository userRepository;

    /**
     * Perfil de un usuario.
     */
    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<UserModel>> getUserProfileByid(@RequestBody UserId body, Authentication auth) {

        UserModel requester = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", 401));

        Long targetId = (body != null && body.getId() != null) ? body.getId() : requester.getId();

        boolean esAdmin = requester.getRoleId() != null
                && "ADMIN".equalsIgnoreCase(requester.getRoleId().getName());

        if (!esAdmin && !requester.getId().equals(targetId)) {
            throw new BusinessException("No puede consultar el perfil de otro usuario", 403);
        }

        UserModel user = userService.getUserById(targetId);
        return new ResponseEntity<>(new Response<>(user, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/all/profile")
    public ResponseEntity<Response<List<UserModel>>> getUserProfiles() {
        return new ResponseEntity<>(
                new Response<>(userService.getUsers(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Da de alta un empleado.
     */
    @PostMapping("/save-profile")
    public ResponseEntity<Response<UserModel>> saveUser(
            @RequestPart("profile") UserModel body,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return new ResponseEntity<>(
                new Response<>(userService.crear(body, file), new StatusCode(200, "Empleado creado")),
                HttpStatus.CREATED);
    }

    /**
     * Actualiza los datos de un empleado.
     */
    @PostMapping("/update-profile")
    public ResponseEntity<Response<UserModel>> updateUser(
            @RequestPart("profile") UserModel body,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return new ResponseEntity<>(
                new Response<>(userService.actualizar(body, file),
                        new StatusCode(200, "Empleado actualizado")),
                HttpStatus.OK);
    }

    /**
     * Cambia la foto 
     */
    @PostMapping(value = "/profile/photo", consumes = { "multipart/form-data" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<UserModel>> updateMyPhoto(
            @RequestPart("file") MultipartFile file, Authentication auth) {

        UserModel requester = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", 401));

        UserModel actualizado = userService.actualizarFotoPropia(requester.getId(), file);

        return new ResponseEntity<>(
                new Response<>(actualizado, new StatusCode(200, "Foto actualizada")), HttpStatus.OK);
    }

    /**
     * Cambia la contraseña de un empleado.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Response<Void>> changePassword(@RequestBody ChangePasswordRequest body) {

        if (body == null || body.getUserId() == null) {
            throw new BusinessException("Falta el identificador del empleado", 400);
        }

        userService.cambiarClave(body.getUserId(), body.getNewPassword());

        return new ResponseEntity<>(
                new Response<>(null, new StatusCode(200, "Contraseña actualizada")), HttpStatus.OK);
    }
}
