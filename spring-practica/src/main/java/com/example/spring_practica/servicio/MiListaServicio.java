package com.example.spring_practica.servicio;

import com.example.spring_practica.excepcion.ArcoException;
import com.example.spring_practica.modelo.EstadoLista;
import com.example.spring_practica.modelo.ListaUsuario;
import com.example.spring_practica.modelo.TipoTitulo;
import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.EstadoListaRepositorio;
import com.example.spring_practica.repositorio.ListaUsuarioRepositorio;
import com.example.spring_practica.repositorio.TipoTituloRepositorio;
import com.example.spring_practica.repositorio.TituloRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Mi lista" de la pagina (C5). Usa las tablas lista_usuario, titulos, estados_lista y tipos_titulo.
 *
 * - Al agregar, si la pelicula o serie todavia no esta en la tabla titulos, se guarda lo esencial
 *   (nombre, sinopsis, anio, calificacion, poster y fondo) con datos de TMDB. Generos, reparto y
 *   plataformas NO se guardan: el detalle se sigue consultando en vivo a TMDB (decision D11).
 * - La pagina solo usa 3 estados de la tabla estados_lista: pendiente (se muestra como "Por ver"),
 *   viendo y completado. en_pausa y abandonado siguen en la tabla pero ya no se aceptan aqui.
 * - Las resenas son la calificacion y la resena de cada fila de lista_usuario, y SOLO cuentan
 *   cuando el titulo esta en "completado".
 */
@Service
public class MiListaServicio {

    private static final int MAX_RESENA = 2000;
    private static final int MAX_SINOPSIS = 4000;
    public static final String COMPLETADO = "completado";
    public static final Set<String> ESTADOS_PERMITIDOS = Set.of("pendiente", "viendo", COMPLETADO);

    private final ListaUsuarioRepositorio lista;
    private final TituloRepositorio titulos;
    private final EstadoListaRepositorio estados;
    private final TipoTituloRepositorio tipos;
    private final UsuarioRepositorio usuarios;
    private final TmdbServicio tmdb;

    public MiListaServicio(ListaUsuarioRepositorio lista, TituloRepositorio titulos,
                           EstadoListaRepositorio estados, TipoTituloRepositorio tipos,
                           UsuarioRepositorio usuarios, TmdbServicio tmdb) {
        this.lista = lista;
        this.titulos = titulos;
        this.estados = estados;
        this.tipos = tipos;
        this.usuarios = usuarios;
        this.tmdb = tmdb;
    }

    // ---------------- Consultar ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Integer usuarioId) {
        validarUsuario(usuarioId);
        Map<Integer, String> clavesEstado = clavesEstado();
        Map<Integer, String> clavesTipo = clavesTipo();
        List<Map<String, Object>> salida = new ArrayList<>();
        for (ListaUsuario fila : lista.findByUsuarioIdOrderByActualizadoEnDesc(usuarioId)) {
            salida.add(aItem(fila, clavesEstado, clavesTipo));
        }
        return salida;
    }

    /** Resenas de todos los usuarios para un titulo (solo filas con calificacion o resena). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resenas(Long tmdbId, String tipo) {
        validarTmdb(tmdbId, tipo);
        TipoTitulo tipoTitulo = tipoPorClave(tipo);
        Titulo titulo = titulos.findByTmdbIdAndTipoId(tmdbId.intValue(), tipoTitulo.getId()).orElse(null);
        List<Map<String, Object>> salida = new ArrayList<>();
        if (titulo == null) {
            return salida; // nadie lo ha agregado todavia
        }

        Map<Integer, String> clavesEstado = clavesEstado();
        List<ListaUsuario> filas = new ArrayList<>();
        Set<Integer> idsUsuarios = new HashSet<>();
        for (ListaUsuario fila : lista.findByTitulo_IdOrderByActualizadoEnDesc(titulo.getId())) {
            if (!COMPLETADO.equals(clavesEstado.get(fila.getEstadoId()))) continue;
            boolean tieneResena = fila.getResena() != null && !fila.getResena().isBlank();
            if (fila.getCalificacion() != null || tieneResena) {
                filas.add(fila);
                idsUsuarios.add(fila.getUsuarioId());
            }
        }

        Map<Integer, String> nombres = new HashMap<>();
        for (Usuario u : usuarios.findAllById(idsUsuarios)) {
            nombres.put(u.getId(), u.getNombre());
        }

        for (ListaUsuario fila : filas) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", fila.getId());
            r.put("usuario", nombres.getOrDefault(fila.getUsuarioId(), "Usuario"));
            r.put("calificacion", fila.getCalificacion());
            r.put("resena", fila.getResena());
            r.put("estado", clavesEstado.get(fila.getEstadoId()));
            r.put("fecha", fila.getActualizadoEn() == null ? null : fila.getActualizadoEn().toString());
            salida.add(r);
        }
        return salida;
    }

    // ---------------- Agregar ----------------

    @Transactional
    public Map<String, Object> agregar(Integer usuarioId, Long tmdbId, String tipo, String estado) {
        validarUsuario(usuarioId);
        validarTmdb(tmdbId, tipo);
        TipoTitulo tipoTitulo = tipoPorClave(tipo);
        EstadoLista estadoLista = estadoPorClave(estado == null ? "pendiente" : estado);

        Titulo titulo = titulos.findByTmdbIdAndTipoId(tmdbId.intValue(), tipoTitulo.getId()).orElse(null);
        if (titulo == null) {
            // Primera vez que alguien agrega este titulo: se guarda lo esencial desde TMDB
            titulo = new Titulo();
            titulo.setTmdbId(tmdbId.intValue());
            titulo.setTipoId(tipoTitulo.getId());
            copiarDeTmdb(titulo, tmdb.detalle(tmdbId, tipo, "MX"));
            titulo = titulos.save(titulo);
        } else if (titulo.getImagenPoster() == null) {
            // Titulos de ejemplo (sin poster): se completan si TMDB responde; si no, se agregan igual
            try {
                copiarDeTmdb(titulo, tmdb.detalle(tmdbId, tipo, "MX"));
                titulo = titulos.save(titulo);
            } catch (ArcoException e) {
                // se queda con los datos que ya tenia
            }
        }

        if (lista.existsByUsuarioIdAndTitulo_Id(usuarioId, titulo.getId())) {
            throw new ArcoException(HttpStatus.CONFLICT, "Este titulo ya esta en tu lista");
        }

        ListaUsuario fila = new ListaUsuario();
        fila.setUsuarioId(usuarioId);
        fila.setTitulo(titulo);
        fila.setEstadoId(estadoLista.getId());
        return aItem(lista.save(fila), clavesEstado(), clavesTipo());
    }

    // ---------------- Actualizar y quitar ----------------

    /**
     * Solo cambia los campos que vengan en el cuerpo: estado, calificacion (0 a 10, o null) y resena.
     * Calificar o escribir resena solo se permite si el titulo queda en "completado".
     */
    @Transactional
    public Map<String, Object> actualizar(Integer id, Integer usuarioId, Map<String, Object> cambios) {
        ListaUsuario fila = filaDelUsuario(id, usuarioId);

        if (cambios.containsKey("estado")) {
            Object valor = cambios.get("estado");
            fila.setEstadoId(estadoPorClave(valor == null ? null : valor.toString()).getId());
        }
        boolean quiereCalificar = cambios.get("calificacion") != null
                && !cambios.get("calificacion").toString().isBlank();
        boolean quiereResenar = cambios.get("resena") != null
                && !cambios.get("resena").toString().isBlank();
        if ((quiereCalificar || quiereResenar)
                && !COMPLETADO.equals(clavesEstado().get(fila.getEstadoId()))) {
            throw new ArcoException(HttpStatus.BAD_REQUEST,
                    "Solo puedes calificar y escribir una resena cuando el titulo esta en Completado");
        }
        if (cambios.containsKey("calificacion")) {
            Integer calificacion = entero(cambios.get("calificacion"), "calificacion");
            if (calificacion != null && (calificacion < 0 || calificacion > 10)) {
                throw new ArcoException(HttpStatus.BAD_REQUEST, "La calificacion va de 0 a 10");
            }
            fila.setCalificacion(calificacion);
        }
        if (cambios.containsKey("resena")) {
            Object valor = cambios.get("resena");
            String resena = valor == null ? null : valor.toString().trim();
            if (resena != null && resena.isEmpty()) resena = null;
            if (resena != null && resena.length() > MAX_RESENA) {
                throw new ArcoException(HttpStatus.BAD_REQUEST, "La resena puede tener maximo " + MAX_RESENA + " caracteres");
            }
            fila.setResena(resena);
        }
        return aItem(lista.save(fila), clavesEstado(), clavesTipo());
    }

    @Transactional
    public void quitar(Integer id, Integer usuarioId) {
        lista.delete(filaDelUsuario(id, usuarioId));
    }

    // ---------------- Internos ----------------

    private ListaUsuario filaDelUsuario(Integer id, Integer usuarioId) {
        validarUsuario(usuarioId);
        ListaUsuario fila = lista.findById(id)
                .orElseThrow(() -> new ArcoException(HttpStatus.NOT_FOUND, "Ese titulo ya no esta en tu lista"));
        if (!fila.getUsuarioId().equals(usuarioId)) {
            throw new ArcoException(HttpStatus.FORBIDDEN, "Ese elemento no es de tu lista");
        }
        return fila;
    }

    private void copiarDeTmdb(Titulo titulo, Map<String, Object> d) {
        Object nombre = d.get("nombre");
        titulo.setNombre(nombre == null ? "Sin titulo" : nombre.toString());
        Object sinopsis = d.get("sinopsis");
        String texto = sinopsis == null ? "" : sinopsis.toString();
        titulo.setSinopsis(texto.length() > MAX_SINOPSIS ? texto.substring(0, MAX_SINOPSIS) : texto);
        titulo.setAnio(d.get("anio") instanceof Number n ? n.intValue() : null);
        titulo.setCalificacionTmdb(d.get("calificacion") instanceof Number n ? n.doubleValue() : null);
        titulo.setImagenPoster(d.get("imagenPoster") == null ? null : d.get("imagenPoster").toString());
        titulo.setImagenFondo(d.get("imagenFondo") == null ? null : d.get("imagenFondo").toString());
    }

    private Map<String, Object> aItem(ListaUsuario fila, Map<Integer, String> clavesEstado,
                                      Map<Integer, String> clavesTipo) {
        Titulo t = fila.getTitulo();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", fila.getId());
        item.put("usuarioId", fila.getUsuarioId());
        item.put("tituloId", t.getId());
        item.put("tmdbId", t.getTmdbId());
        item.put("tipo", clavesTipo.getOrDefault(t.getTipoId(), "pelicula"));
        item.put("nombre", t.getNombre());
        item.put("anio", t.getAnio());
        item.put("imagenPoster", t.getImagenPoster());
        item.put("estado", clavesEstado.get(fila.getEstadoId()));
        item.put("calificacion", fila.getCalificacion());
        item.put("resena", fila.getResena());
        item.put("creadoEn", fila.getCreadoEn() == null ? null : fila.getCreadoEn().toString());
        return item;
    }

    private void validarUsuario(Integer usuarioId) {
        if (usuarioId == null) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Falta usuarioId");
        }
        if (!usuarios.existsById(usuarioId)) {
            throw new ArcoException(HttpStatus.UNAUTHORIZED, "Tu usuario ya no existe. Inicia sesion otra vez.");
        }
    }

    private void validarTmdb(Long tmdbId, String tipo) {
        if (tmdbId == null || tmdbId <= 0 || tmdbId > Integer.MAX_VALUE) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "ID de TMDB invalido");
        }
        if (!"pelicula".equals(tipo) && !"serie".equals(tipo)) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Tipo invalido (pelicula | serie)");
        }
    }

    private TipoTitulo tipoPorClave(String clave) {
        return tipos.findByClave(clave).orElseThrow(() -> new ArcoException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Falta el tipo '" + clave + "' en la tabla tipos_titulo"));
    }

    private EstadoLista estadoPorClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Falta el estado");
        }
        String limpia = clave.trim();
        if (!ESTADOS_PERMITIDOS.contains(limpia)) {
            throw new ArcoException(HttpStatus.BAD_REQUEST,
                    "Estado invalido: " + clave + ". Usa pendiente (por ver), viendo o completado");
        }
        return estados.findByClave(limpia).orElseThrow(() -> new ArcoException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Falta el estado '" + limpia + "' en la tabla estados_lista"));
    }

    private Map<Integer, String> clavesEstado() {
        Map<Integer, String> mapa = new HashMap<>();
        for (EstadoLista e : estados.findAll()) mapa.put(e.getId(), e.getClave());
        return mapa;
    }

    private Map<Integer, String> clavesTipo() {
        Map<Integer, String> mapa = new HashMap<>();
        for (TipoTitulo t : tipos.findAll()) mapa.put(t.getId(), t.getClave());
        return mapa;
    }

    private Integer entero(Object valor, String campo) {
        if (valor == null) return null;
        if (valor instanceof Number n) return n.intValue();
        String texto = valor.toString().trim();
        if (texto.isEmpty()) return null;
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El campo " + campo + " debe ser un numero");
        }
    }
}
