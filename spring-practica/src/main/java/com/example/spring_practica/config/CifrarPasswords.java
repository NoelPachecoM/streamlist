package com.example.spring_practica.config;

import com.example.spring_practica.modelo.RegistroPendiente;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.RegistroPendienteRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import com.example.spring_practica.servicio.Contrasenas;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, cifra con BCrypt las contrasenas que todavia esten en texto plano
 * (usuarios creados antes de este cambio y registros pendientes). Si ya estan cifradas, no hace nada.
 */
@Component
public class CifrarPasswords implements CommandLineRunner {

    private final UsuarioRepositorio usuarios;
    private final RegistroPendienteRepositorio pendientes;
    private final Contrasenas contrasenas;

    public CifrarPasswords(UsuarioRepositorio usuarios, RegistroPendienteRepositorio pendientes,
                           Contrasenas contrasenas) {
        this.usuarios = usuarios;
        this.pendientes = pendientes;
        this.contrasenas = contrasenas;
    }

    @Override
    public void run(String... args) {
        int cifradas = 0;
        for (Usuario u : usuarios.findAll()) {
            if (u.getPassword() != null && !contrasenas.esHash(u.getPassword())) {
                try {
                    u.setPassword(contrasenas.cifrar(u.getPassword()));
                    usuarios.save(u);
                    cifradas++;
                } catch (RuntimeException e) {
                    System.out.println("No se pudo cifrar la password del usuario " + u.getId() + ": " + e.getMessage());
                }
            }
        }
        for (RegistroPendiente r : pendientes.findAll()) {
            if (r.getPassword() != null && !contrasenas.esHash(r.getPassword())) {
                try {
                    r.setPassword(contrasenas.cifrar(r.getPassword()));
                    pendientes.save(r);
                } catch (RuntimeException e) {
                    pendientes.delete(r);
                }
            }
        }
        if (cifradas > 0) {
            System.out.println("Passwords cifradas con BCrypt: " + cifradas + " usuario(s).");
        }
    }
}
