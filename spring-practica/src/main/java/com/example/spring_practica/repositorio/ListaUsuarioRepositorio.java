package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.ListaUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ListaUsuarioRepositorio extends JpaRepository<ListaUsuario, Integer> {

    boolean existsByUsuarioIdAndTitulo_Id(Integer usuarioId, Integer tituloId);

    Optional<ListaUsuario> findByUsuarioIdAndTitulo_Id(Integer usuarioId, Integer tituloId);

    Page<ListaUsuario> findByUsuarioId(Integer usuarioId, Pageable pageable);

    // Mi lista de un usuario (lo mas reciente primero)
    List<ListaUsuario> findByUsuarioIdOrderByActualizadoEnDesc(Integer usuarioId);

    // Todas las filas de un titulo (para la seccion de resenas del detalle)
    List<ListaUsuario> findByTitulo_IdOrderByActualizadoEnDesc(Integer tituloId);

    // Para pasar los estados que ya no se usan (en_pausa, abandonado) a "viendo"
    List<ListaUsuario> findByEstadoIdIn(Collection<Integer> estadoIds);
}
