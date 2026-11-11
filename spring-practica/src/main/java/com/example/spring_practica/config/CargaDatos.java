package com.example.spring_practica.config;

import com.example.spring_practica.modelo.*;
import com.example.spring_practica.repositorio.*;
import com.example.spring_practica.servicio.Contrasenas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Carga datos de ejemplo al arrancar la aplicacion.
 * Cada tabla se llena solo si esta vacia, asi que reiniciar la app no duplica nada.
 */
@Component
public class CargaDatos implements CommandLineRunner {

    private static final String CORREO_PRUEBA = "noelpa847@gmail.com";
    private static final String CORREO_ADMIN = CORREO_PRUEBA;

    private final GeneroRepositorio generoRepo;
    private final EstadoListaRepositorio estadoRepo;
    private final PersonaRepositorio personaRepo;
    private final PlataformaRepositorio plataformaRepo;
    private final TipoTituloRepositorio tipoRepo;
    private final RolRepositorio rolRepo;
    private final UsuarioRepositorio usuarioRepo;
    private final SolicitudArcoRepositorio solicitudRepo;
    private final Contrasenas contrasenas;

    // Contrasena de la cuenta de prueba (variable PRUEBA_PASSWORD en Render; "root" en tu computadora)
    @Value("${datos.password-prueba:root}")
    private String passwordPrueba;

    public CargaDatos(GeneroRepositorio generoRepo,
                      EstadoListaRepositorio estadoRepo,
                      PersonaRepositorio personaRepo,
                      PlataformaRepositorio plataformaRepo,
                      TipoTituloRepositorio tipoRepo,
                      RolRepositorio rolRepo,
                      UsuarioRepositorio usuarioRepo,
                      SolicitudArcoRepositorio solicitudRepo,
                      Contrasenas contrasenas) {
        this.generoRepo = generoRepo;
        this.estadoRepo = estadoRepo;
        this.personaRepo = personaRepo;
        this.plataformaRepo = plataformaRepo;
        this.tipoRepo = tipoRepo;
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.solicitudRepo = solicitudRepo;
        this.contrasenas = contrasenas;
    }

    @Override
    public void run(String... args) {
        cargarGeneros();
        cargarEstados();
        cargarPersonas();
        cargarPlataformas();
        cargarTipos();
        cargarRoles();
        cargarUsuarioPrueba();
        asegurarAdministrador();
        cargarSolicitudesEjemplo();
        System.out.println("Carga de datos terminada.");
    }

    private void cargarGeneros() {
        if (generoRepo.count() > 0) return;
        generoRepo.saveAll(List.of(
                new Genero(null, 28, "Accion"),
                new Genero(null, 12, "Aventura"),
                new Genero(null, 16, "Animacion"),
                new Genero(null, 35, "Comedia"),
                new Genero(null, 80, "Crimen"),
                new Genero(null, 99, "Documental"),
                new Genero(null, 18, "Drama"),
                new Genero(null, 10751, "Familia"),
                new Genero(null, 14, "Fantasia"),
                new Genero(null, 27, "Terror"),
                new Genero(null, 10749, "Romance"),
                new Genero(null, 878, "Ciencia ficcion"),
                new Genero(null, 53, "Suspenso"),
                new Genero(null, 10752, "Belica")));
    }

    private void cargarEstados() {
        if (estadoRepo.count() > 0) return;
        estadoRepo.saveAll(List.of(
                new EstadoLista(null, "pendiente", "Pendiente"),
                new EstadoLista(null, "viendo", "Viendo"),
                new EstadoLista(null, "completado", "Completado"),
                new EstadoLista(null, "en_pausa", "En pausa"),
                new EstadoLista(null, "abandonado", "Abandonado")));
    }

    private void cargarPersonas() {
        if (personaRepo.count() > 0) return;
        personaRepo.saveAll(List.of(
                new Persona(null, 1, "Gael Garcia Bernal", "Masculino", LocalDate.of(1978, 11, 30), "Mexico"),
                new Persona(null, 2, "Salma Hayek", "Femenino", LocalDate.of(1966, 9, 2), "Mexico"),
                new Persona(null, 3, "Diego Luna", "Masculino", LocalDate.of(1979, 12, 29), "Mexico"),
                new Persona(null, 4, "Yalitza Aparicio", "Femenino", LocalDate.of(1993, 12, 11), "Mexico"),
                new Persona(null, 5, "Guillermo del Toro", "Masculino", LocalDate.of(1964, 10, 9), "Mexico"),
                new Persona(null, 6, "Alfonso Cuaron", "Masculino", LocalDate.of(1961, 11, 28), "Mexico"),
                new Persona(null, 7, "Eugenio Derbez", "Masculino", LocalDate.of(1961, 9, 2), "Mexico"),
                new Persona(null, 8, "Ana de Armas", "Femenino", LocalDate.of(1988, 4, 30), "Cuba"),
                new Persona(null, 9, "Pedro Pascal", "Masculino", LocalDate.of(1975, 4, 2), "Chile"),
                new Persona(null, 10, "Tom Hanks", "Masculino", LocalDate.of(1956, 7, 9), "Estados Unidos"),
                new Persona(null, 11, "Meryl Streep", "Femenino", LocalDate.of(1949, 6, 22), "Estados Unidos"),
                new Persona(null, 12, "Scarlett Johansson", "Femenino", LocalDate.of(1984, 11, 22), "Estados Unidos")));
    }

    private void cargarPlataformas() {
        if (plataformaRepo.count() > 0) return;
        plataformaRepo.saveAll(List.of(
                new Plataforma(null, 8, "Netflix", "Estados Unidos", 219.0),
                new Plataforma(null, 9, "Amazon Prime Video", "Estados Unidos", 99.0),
                new Plataforma(null, 337, "Disney Plus", "Estados Unidos", 159.0),
                new Plataforma(null, 1899, "Max", "Estados Unidos", 149.0),
                new Plataforma(null, 350, "Apple TV Plus", "Estados Unidos", 99.0),
                new Plataforma(null, 531, "Paramount Plus", "Estados Unidos", 99.0)));
    }

    private void cargarTipos() {
        if (tipoRepo.count() > 0) return;
        tipoRepo.saveAll(List.of(
                new TipoTitulo(null, "pelicula", "Pelicula"),
                new TipoTitulo(null, "serie", "Serie"),
                new TipoTitulo(null, "documental", "Documental"),
                new TipoTitulo(null, "anime", "Anime")));
    }

    private void cargarRoles() {
        if (rolRepo.count() > 0) return;
        rolRepo.saveAll(List.of(
                new Rol(null, "ROLE_ADMIN"),
                new Rol(null, "ROLE_CLIENTE")));
    }

    // Perfil fijo para probar los derechos ARCO con un correo real
    private void cargarUsuarioPrueba() {
        if (usuarioRepo.existsByEmail(CORREO_PRUEBA)) return;
        Rol rolCliente = rolRepo.findAll().stream()
                .filter(r -> r.getNombre().equals("ROLE_CLIENTE"))
                .findFirst()
                .orElse(null);
        if (rolCliente == null) return;
        usuarioRepo.save(new Usuario(
                null,
                "noelTesfaye",
                CORREO_PRUEBA,
                contrasenas.cifrar(passwordPrueba), // se guarda cifrada
                "3532487266",
                true,
                LocalDateTime.now(),
                rolCliente));
    }

    // El rol de cada persona se elige en la base (columna rol_id de usuarios, tabla roles) o desde
    // el panel de Administracion. Spring ya NO fuerza a nadie como administrador: solo si no
    // existe ningun ROLE_ADMIN (por ejemplo, despues de vaciar usuarios) la cuenta de prueba
    // CORREO_ADMIN pasa a ser administradora, para que nunca te quedes sin acceso al panel.
    private void asegurarAdministrador() {
        if (usuarioRepo.existsByRol_Nombre("ROLE_ADMIN")) return;
        Usuario usuario = usuarioRepo.findByEmail(CORREO_ADMIN).orElse(null);
        if (usuario == null) return;
        if (usuario.getRol() != null && "ROLE_ADMIN".equals(usuario.getRol().getNombre())) return;
        Rol rolAdmin = rolRepo.findAll().stream()
                .filter(r -> "ROLE_ADMIN".equals(r.getNombre()))
                .findFirst()
                .orElse(null);
        if (rolAdmin == null) return;
        usuario.setRol(rolAdmin);
        usuarioRepo.save(usuario);
    }

    // Dos solicitudes de ejemplo para ver la tabla con datos
    private void cargarSolicitudesEjemplo() {
        if (solicitudRepo.count() > 0) return;
        Usuario usuario = usuarioRepo.findByEmail(CORREO_PRUEBA).orElse(null);
        if (usuario == null) return;
        LocalDateTime ahora = LocalDateTime.now();
        solicitudRepo.saveAll(List.of(
                new SolicitudArco(null, usuario, "ACCESO",
                        "Solicito conocer los datos personales que tienen de mi",
                        "COMPLETADO", ahora.minusDays(5), ahora.minusDays(3)),
                new SolicitudArco(null, usuario, "RECTIFICACION",
                        "Solicito corregir mi numero de telefono",
                        "PENDIENTE", ahora.minusDays(1), null)));
    }
}
