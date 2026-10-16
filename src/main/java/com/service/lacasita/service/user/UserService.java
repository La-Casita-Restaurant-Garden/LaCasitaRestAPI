package com.service.lacasita.service.user;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.service.lacasita.entity.UserModel;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.auth.AuthService;
import com.service.lacasita.service.s3.S3Service;

/**
 * Empleados.
 * @author Jason Nunez
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Un empleado recién dado de alta nace activo. */
    private static final String ESTADO_POR_DEFECTO = "ACTIVE";

    private static final int MIN_CLAVE = 6;

    @Autowired
    private UserRespository userRespository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private AuthService authService;

    @Transactional(readOnly = true)
    public UserModel getUserById(Long id) {
        return userRespository.findById(id)
                .orElseThrow(() -> new BusinessException("Empleado no encontrado: " + id, 404));
    }

    @Transactional(readOnly = true)
    public List<UserModel> getUsers() {
        return userRespository.findAllByOrderByName();
    }

    // ---------------------------------------------------------------------
    // Alta
    // ---------------------------------------------------------------------

    /**
     * Da de alta un empleado.
     */
    @Transactional
    public UserModel crear(UserModel body, MultipartFile file) {

        exigir(body.getName(), "nombre");
        exigir(body.getUsername(), "usuario");

        if (body.getRoleId() == null || body.getRoleId().getId() == null) {
            throw new BusinessException("El empleado necesita un rol", 400);
        }

        String clave = body.getPassword();
        if (clave == null || clave.trim().length() < MIN_CLAVE) {
            throw new BusinessException(
                    "La contraseña debe tener al menos %d caracteres".formatted(MIN_CLAVE), 400);
        }

        String username = body.getUsername().trim();
        if (userRespository.findByUsername(username).isPresent()) {
            throw new BusinessException("Ya hay un empleado con el usuario " + username, 409);
        }

        String email = normalizar(body.getEmail());
        if (email != null && userRespository.findByEmail(email).isPresent()) {
            throw new BusinessException("Ya hay un empleado con el correo " + email, 409);
        }

        UserModel nuevo = new UserModel();
        nuevo.setName(body.getName().trim());
        nuevo.setLastname(normalizar(body.getLastname()));
        nuevo.setUsername(username);
        nuevo.setEmail(email);
        nuevo.setPhone(normalizar(body.getPhone()));
        nuevo.setRoleId(body.getRoleId());
        nuevo.setStatus(body.getStatus() != null ? body.getStatus() : ESTADO_POR_DEFECTO);
        nuevo.setPassword(authService.decodePassword(clave.trim()));
        nuevo.setImage(subirImagen(file, null));

        return userRespository.save(nuevo);
    }

    /**
     * Actualiza los datos de un empleado.
     */
    @Transactional
    public UserModel actualizar(UserModel body, MultipartFile file) {

        if (body.getId() == null) {
            throw new BusinessException("Falta el identificador del empleado", 400);
        }

        UserModel actual = getUserById(body.getId());

        exigir(body.getName(), "nombre");

        String username = normalizar(body.getUsername());
        if (username != null && !username.equals(actual.getUsername())) {
            if (userRespository.findByUsername(username).isPresent()) {
                throw new BusinessException("Ya hay un empleado con el usuario " + username, 409);
            }
            actual.setUsername(username);
        }

        String email = normalizar(body.getEmail());
        if (email != null && !email.equalsIgnoreCase(actual.getEmail())
                && userRespository.findByEmail(email).isPresent()) {
            throw new BusinessException("Ya hay un empleado con el correo " + email, 409);
        }

        actual.setName(body.getName().trim());
        actual.setLastname(normalizar(body.getLastname()));
        actual.setEmail(email);
        actual.setPhone(normalizar(body.getPhone()));

        if (body.getRoleId() != null) actual.setRoleId(body.getRoleId());
        if (body.getStatus() != null) actual.setStatus(body.getStatus());

        actual.setImage(subirImagen(file, actual.getImage()));

        return userRespository.save(actual);
    }

    /** Cambia únicamente la foto de un usuario: lo que puede pedir cualquier autenticado sobre sí mismo. */
    @Transactional
    public UserModel actualizarFotoPropia(Long userId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Selecciona una imagen", 400);
        }

        UserModel actual = getUserById(userId);
        actual.setImage(subirImagen(file, actual.getImage()));

        return userRespository.save(actual);
    }

    /**
     * Cambia la contraseña de un empleado.
     */
    @Transactional
    public void cambiarClave(Long userId, String nuevaClave) {

        if (nuevaClave == null || nuevaClave.trim().length() < MIN_CLAVE) {
            throw new BusinessException(
                    "La contraseña debe tener al menos %d caracteres".formatted(MIN_CLAVE), 400);
        }

        UserModel usuario = getUserById(userId);
        usuario.setPassword(authService.decodePassword(nuevaClave.trim()));
        userRespository.save(usuario);

        log.info("Contraseña cambiada para el empleado {}", userId);
    }

    // ---------------------------------------------------------------------

    /** Sube la imagen nueva y borra la anterior; sin fichero, conserva la que hubiera. */
    private String subirImagen(MultipartFile file, String actual) {

        if (file == null || file.isEmpty()) return actual;

        try {
            String nueva = s3Service.uploadFileToS3(file, "profile");
            s3Service.deleteFileFromS3(actual);
            return nueva;
        } catch (IOException e) {
            throw new BusinessException("No se pudo subir la imagen: " + e.getMessage(), 500);
        }
    }

    private void exigir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("El empleado necesita un " + campo, 400);
        }
    }

    /** Cadena vacía y solo espacios valen lo mismo que ausente: null. */
    private String normalizar(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
