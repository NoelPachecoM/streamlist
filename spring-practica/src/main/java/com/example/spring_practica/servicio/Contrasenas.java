package com.example.spring_practica.servicio;

import com.example.spring_practica.excepcion.ArcoException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Cifrado de contrasenas con BCrypt. En la base ya no se guarda la contrasena, sino un "hash"
 * como $2a$10$N9qo8uLOickgx2ZMRZoMye... del que no se puede sacar la contrasena original.
 * Para iniciar sesion se compara lo que escribe el usuario contra ese hash.
 */
@Service
public class Contrasenas {

    // BCrypt solo usa los primeros 72 bytes
    private static final int MAX_BYTES = 72;

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);

    public String cifrar(String plana) {
        if (plana == null || plana.isEmpty()) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Falta la password");
        }
        if (plana.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "La password no puede pasar de 72 caracteres");
        }
        return bcrypt.encode(plana);
    }

    /** true si la contrasena escrita corresponde a la guardada. */
    public boolean coincide(String plana, String guardada) {
        if (plana == null || guardada == null) return false;
        if (plana.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return false;
        if (esHash(guardada)) {
            try {
                return bcrypt.matches(plana, guardada);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        // Cuenta vieja que todavia no se cifra (CifrarPasswords la cifra al arrancar Spring)
        return MessageDigest.isEqual(plana.getBytes(StandardCharsets.UTF_8),
                guardada.getBytes(StandardCharsets.UTF_8));
    }

    public boolean esHash(String guardada) {
        return guardada != null && guardada.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }

    /** Cifra solo si todavia no es un hash (para no cifrar dos veces). */
    public String cifrarSiHaceFalta(String valor) {
        return esHash(valor) ? valor : cifrar(valor);
    }
}
