package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Actor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActorRepositorio extends JpaRepository<Actor, Integer> {

}
