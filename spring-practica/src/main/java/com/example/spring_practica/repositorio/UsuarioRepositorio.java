package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // Hay al menos un usuario con ese rol (ej. ROLE_ADMIN)
    boolean existsByRol_Nombre(String nombreRol);

    // Usuarios con ese nombre (el nombre no es unico)
    List<Usuario> findByNombre(String nombre);

    // El nombre no es unico, por eso devuelve una lista
    List<Usuario> findByNombreAndPassword(String nombre, String password);
}
