package com.service.lacasita.service.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;

@Service
public class AuthService {

    private static final String CHAR_LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[{]};:,.<>?";

    private static final String PASSWORD_ALLOW = CHAR_LOWERCASE + CHAR_UPPERCASE + DIGITS + SPECIAL_CHARS;

    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private UserRespository userRespository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Autentica a un usuario comparando el email y la contraseña ingresados con los almacenados en la base de datos.
     * @param email
     * @param password
     * @return
     */
    public Optional<UserModel> authenticate(String email, String password) {

        Optional<UserModel> userdb = userRespository.findByUsername(email);

        if (userdb.isPresent()) {
            UserModel user = userdb.get();
            if (mathPassword(password, user.getPassword()).equals(user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();

    }

    /**
     * Encripta la contraseña utilizando el PasswordEncoder de Spring Security
     * 
     * @param password
     * @return
     */
    public String decodePassword(String password) {
        return passwordEncoder.encode(password);

    }

    /**
     * Compara la contraseña ingresada con la contraseña encriptada almacenada en
     * la base de datos
     * 
     * @param password
     * @param hashPassword
     * @return
     */
    public String mathPassword(String password, String hashPassword) {

        if (passwordEncoder.matches(password, hashPassword)) {
            return hashPassword;
        }
        return "Not Match";
    }

    /**
     * Genera una contraseña aleatoria
     * 
     * @param length
     * @return
     */
    public String generateRandomPassword(int length) {
        if (length < 6) {
            throw new IllegalArgumentException("La longitud de la contraseña debe ser de al menos 6 caracteres.");
        }

        List<Character> passwordChars = new ArrayList<>();

        passwordChars.add(CHAR_LOWERCASE.charAt(random.nextInt(CHAR_LOWERCASE.length())));
        passwordChars.add(CHAR_UPPERCASE.charAt(random.nextInt(CHAR_UPPERCASE.length())));
        passwordChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        passwordChars.add(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        for (int i = 4; i < length; i++) {
            passwordChars.add(PASSWORD_ALLOW.charAt(random.nextInt(PASSWORD_ALLOW.length())));
        }

        Collections.shuffle(passwordChars, random);

        StringBuilder password = new StringBuilder(length);
        for (char c : passwordChars) {
            password.append(c);
        }

        return password.toString();
    }
}