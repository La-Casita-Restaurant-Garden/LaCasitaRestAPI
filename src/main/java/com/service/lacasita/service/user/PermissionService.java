package com.service.lacasita.service.user;

import com.service.lacasita.dto.PermissionDTO;
import com.service.lacasita.dto.UserPermissionsDTO;
import com.service.lacasita.entity.Permission;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.entity.UserPermissionOverride;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.PermissionRepository;
import com.service.lacasita.repository.UserPermissionOverrideRepository;
import com.service.lacasita.repository.UserRespository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permisos por rol, con excepciones por usuario.
 *
 * <p>El acceso efectivo se calcula así:
 *
 * <pre>
 *   base      = matriz del rol            (tabla `permisos`)
 *   excepción = fila del usuario vigente  (tabla `permisos_usuario`)
 *
 *   efectivo  = base, sobrescrita campo a campo por la excepción
 *               (NULL en la excepción = hereda la base)
 * </pre>
 *
 * <p>Es lo que permite dar el dashboard a UN mesero sin dárselo a todos, sin
 * inventar un rol para una sola persona.
 *
 * @author Jason Nunez
 */
@Service
public class PermissionService {

    /** Rol con acceso total; se salta la matriz por completo. */
    public static final String ROL_ADMIN = "ADMIN";

    private final PermissionRepository permissionRepository;
    private final UserRespository userRepository;
    private final UserPermissionOverrideRepository overrideRepository;

    public PermissionService(PermissionRepository permissionRepository,
            UserRespository userRepository,
            UserPermissionOverrideRepository overrideRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.overrideRepository = overrideRepository;
    }

    /**
     * Permisos efectivos del usuario: los de su rol más sus excepciones vigentes.
     *
     * <p>Devuelve solo los módulos con lectura efectiva, que es lo que el frontend
     * necesita para decidir qué mostrar.
     */
    @Cacheable(value = "userPermissions", key = "#userId")
    @Transactional(readOnly = true)
    public UserPermissionsDTO getUserPermissions(Long userId) {

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + userId, 404));

        String roleName = user.getRoleId() != null ? user.getRoleId().getName() : null;

        List<PermissionDTO> efectivos = calcularEfectivos(user).values().stream()
                .filter(p -> Boolean.TRUE.equals(p.getCanRead()))
                .collect(Collectors.toList());

        return new UserPermissionsDTO(roleName, efectivos);
    }

    /**
     * Permiso efectivo por módulo, incluidos los módulos sin lectura.
     *
     * <p>Se parte de TODAS las filas del rol, no solo las legibles: una excepción
     * puede conceder escritura sobre un módulo que el rol tiene a false, y hace
     * falta la fila base para saber su etiqueta.
     */
    private Map<String, PermissionDTO> calcularEfectivos(UserModel user) {

        Map<String, PermissionDTO> efectivos = new LinkedHashMap<>();

        Long roleId = user.getRoleId() != null ? user.getRoleId().getId() : null;
        if (roleId != null) {
            for (Permission p : permissionRepository.findByRoleId(roleId)) {
                efectivos.put(p.getModuleKey(), new PermissionDTO(
                        p.getModuleKey(),
                        p.getModuleLabel(),
                        Boolean.TRUE.equals(p.getCanRead()),
                        Boolean.TRUE.equals(p.getCanWrite()),
                        Boolean.TRUE.equals(p.getCanDelete())));
            }
        }

        // Las excepciones vencidas las descarta ya la consulta.
        for (UserPermissionOverride o : overrideRepository.findVigentesByUser(user.getId(), Instant.now())) {

            PermissionDTO base = efectivos.get(o.getModuleKey());

            if (base == null) {
                // Módulo que el rol no tiene en absoluto: la excepción lo crea.
                // Lo que quede en null hereda de "sin permiso", es decir false.
                efectivos.put(o.getModuleKey(), new PermissionDTO(
                        o.getModuleKey(),
                        o.getModuleKey(),
                        Boolean.TRUE.equals(o.getCanRead()),
                        Boolean.TRUE.equals(o.getCanWrite()),
                        Boolean.TRUE.equals(o.getCanDelete())));
                continue;
            }

            // null en la excepción = hereda; true/false = decide.
            if (o.getCanRead() != null)   base.setCanRead(o.getCanRead());
            if (o.getCanWrite() != null)  base.setCanWrite(o.getCanWrite());
            if (o.getCanDelete() != null) base.setCanDelete(o.getCanDelete());
        }

        return efectivos;
    }

    /**
     * ¿Puede el usuario realizar la acción sobre el módulo?
     *
     * <p>Es el método que consulta {@code PermissionChecker} desde las anotaciones
     * {@code @PreAuthorize} de los controladores.
     *
     * @param action read, write o delete
     */
    /**
     * Si el usuario es administrador.
     *
     * <p>Se compara sin distinguir mayúsculas, igual que {@link #puede}: el nombre
     * del rol viene de una tabla y basta con que alguien lo guarde como 'Admin'
     * para que una comparación exacta deje fuera al único que debía entrar.
     */
    @Transactional(readOnly = true)
    public boolean esAdmin(String username) {

        return userRepository.findByUsername(username)
                .map(u -> u.getRoleId() != null && ROL_ADMIN.equalsIgnoreCase(u.getRoleId().getName()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean puede(String username, String moduleKey, String action) {

        UserModel user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getRoleId() != null && ROL_ADMIN.equalsIgnoreCase(user.getRoleId().getName())) {
            return true;
        }

        PermissionDTO permiso = calcularEfectivos(user).get(moduleKey);
        if (permiso == null) {
            return false;
        }

        return switch (action == null ? "" : action.toLowerCase()) {
            case "write"  -> Boolean.TRUE.equals(permiso.getCanWrite());
            case "delete" -> Boolean.TRUE.equals(permiso.getCanDelete());
            case "read"   -> Boolean.TRUE.equals(permiso.getCanRead());
            // Una acción desconocida deniega. Un typo en una anotación debe cerrar
            // la puerta, no abrirla.
            default -> false;
        };
    }

    // ---------------------------------------------------------------------
    // Administración de la matriz por rol
    // ---------------------------------------------------------------------

    @Cacheable(value = "rolePermissions", key = "#roleId")
    @Transactional(readOnly = true)
    public List<PermissionDTO> getPermissionsByRole(Long roleId) {
        return permissionRepository.findByRoleId(roleId).stream()
                .map(p -> new PermissionDTO(
                        p.getModuleKey(),
                        p.getModuleLabel(),
                        p.getCanRead(),
                        p.getCanWrite(),
                        p.getCanDelete()))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = { "userPermissions", "rolePermissions" }, allEntries = true)
    @Transactional
    public Permission savePermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Transactional
    @CacheEvict(value = { "userPermissions", "rolePermissions" }, allEntries = true)
    public List<Permission> saveAllPermissions(List<Permission> permissions) {

        List<Permission> result = new ArrayList<>();

        for (Permission incoming : permissions) {
            Long roleId = incoming.getRole().getId();
            String moduleKey = incoming.getModuleKey();

            Permission existing = permissionRepository.findByRoleIdAndModuleKey(roleId, moduleKey);

            if (existing != null) {
                existing.setModuleLabel(incoming.getModuleLabel());
                existing.setCanRead(incoming.getCanRead());
                existing.setCanWrite(incoming.getCanWrite());
                existing.setCanDelete(incoming.getCanDelete());
                result.add(permissionRepository.save(existing));
            } else {
                result.add(permissionRepository.save(incoming));
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------
    // Excepciones por usuario
    // ---------------------------------------------------------------------

    /** Todas las excepciones de un usuario, vigentes o no, para administrarlas. */
    @Transactional(readOnly = true)
    public List<UserPermissionOverride> getOverrides(Long userId) {
        return overrideRepository.findByUserId(userId);
    }

    /** Excepciones vigentes de todo el personal: repaso periódico. */
    @Transactional(readOnly = true)
    public List<UserPermissionOverride> getAllActiveOverrides() {
        return overrideRepository.findTodasVigentes(Instant.now());
    }

    /**
     * Crea o actualiza una excepción.
     *
     * <p>El evict del caché es obligatorio: {@code getUserPermissions} está cacheado
     * por usuario, así que sin esto la concesión no se notaría hasta que el caché
     * expirase.
     */
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public UserPermissionOverride saveOverride(UserPermissionOverride override, Long adminId) {

        if (override.getUserId() == null || override.getModuleKey() == null) {
            throw new BusinessException("userId y moduleKey son obligatorios", 400);
        }

        if (override.getCanRead() == null && override.getCanWrite() == null
                && override.getCanDelete() == null) {
            throw new BusinessException(
                    "La excepción no cambia nada: informe al menos un permiso", 400);
        }

        if (override.getExpiresAt() != null && override.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("La fecha de caducidad ya pasó", 400);
        }

        // Una excepción sobre un ADMIN no tiene sentido: ya tiene acceso total y
        // daría la falsa impresión de estar restringiéndolo.
        UserModel target = userRepository.findById(override.getUserId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", 404));

        if (target.getRoleId() != null && ROL_ADMIN.equalsIgnoreCase(target.getRoleId().getName())) {
            throw new BusinessException(
                    "ADMIN ya tiene acceso total: una excepción no tendría efecto", 400);
        }

        override.setCreatedBy(adminId);
        return overrideRepository.save(override);
    }

    /** Revoca una excepción: el usuario vuelve a lo que diga su rol. */
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void deleteOverride(Long userId, String moduleKey) {
        if (overrideRepository.deleteByUserAndModule(userId, moduleKey) == 0) {
            throw new BusinessException("No existe esa excepción", 404);
        }
    }
}
