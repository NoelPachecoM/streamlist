package com.example.spring_practica.servicio;

import com.example.spring_practica.excepcion.ArcoException;
import com.example.spring_practica.modelo.CacheTmdb;
import com.example.spring_practica.repositorio.CacheTmdbRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Busca peliculas y series en TMDB (https://www.themoviedb.org) usando el token de lectura
 * guardado en application-local.properties (tmdb.token). Es la misma logica que tenia la
 * pagina en Vercel (AdaptadorTMDB.ts), pero ahora todo pasa por Spring.
 *
 * La respuesta usa los mismos nombres de campo que el Angular ya espera
 * (tmdbId, tipo, nombre, sinopsis, anio, calificacion, imagenPoster, ...).
 *
 * CACHE: cada respuesta de TMDB se guarda en la tabla cache_tmdb. Si se vuelve a pedir lo mismo
 * antes de que venza, se usa la copia de la base y no se llama a TMDB:
 *   detalle de un titulo: 7 dias | busquedas: 1 dia | tendencias: 6 horas.
 * Si TMDB falla y hay una copia vencida, se usa esa copia.
 * Las imagenes (posters, fotos) NO usan el token: el navegador las pide directo a image.tmdb.org.
 */
@Service
@PropertySource(value = "classpath:application-local.properties", ignoreResourceNotFound = true)
public class TmdbServicio {

    private static final String URL_API = "https://api.themoviedb.org/3";
    private static final String URL_IMG = "https://image.tmdb.org/t/p";
    private static final String IDIOMA = "es-MX";
    private static final ParameterizedTypeReference<Map<String, Object>> TIPO_MAPA =
            new ParameterizedTypeReference<>() {};

    private static final Duration VIGENCIA_DETALLE = Duration.ofDays(7);
    private static final Duration VIGENCIA_BUSQUEDA = Duration.ofDays(1);
    private static final Duration VIGENCIA_TENDENCIAS = Duration.ofHours(6);

    // Al leer la cache solo se aceptan clases basicas de Java (mapas, listas, textos y numeros)
    private static final ObjectInputFilter FILTRO_CACHE =
            ObjectInputFilter.Config.createFilter("java.util.*;java.lang.*;java.math.*;!*");

    private final String token;
    private final RestClient cliente;
    private final CacheTmdbRepositorio cache;

    public TmdbServicio(@Value("${tmdb.token:}") String token, CacheTmdbRepositorio cache) {
        this.token = token == null ? "" : token.trim();
        this.cache = cache;

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(8));

        this.cliente = RestClient.builder()
                .baseUrl(URL_API)
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
                .build();
    }

    // ---------------- Busqueda ----------------

    public Map<String, Object> buscar(String consulta, int pagina) {
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.length() < 2) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "La busqueda debe tener al menos 2 caracteres");
        }
        if (texto.length() > 100) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "La busqueda es demasiado larga");
        }
        if (pagina < 1 || pagina > 500) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Pagina invalida");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", texto);
        params.put("page", String.valueOf(pagina));
        params.put("include_adult", "false");
        Map<String, Object> datos = pedir("/search/multi", params);

        List<Map<String, Object>> resultados = new ArrayList<>();
        for (Map<String, Object> r : lista(datos, "results")) {
            String media = texto(r, "media_type");
            if ("movie".equals(media)) {
                resultados.add(aTitulo(r, "pelicula"));
            } else if ("tv".equals(media)) {
                resultados.add(aTitulo(r, "serie"));
            }
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("pagina", entero(datos, "page", pagina));
        respuesta.put("totalPaginas", entero(datos, "total_pages", 0));
        respuesta.put("totalResultados", entero(datos, "total_results", 0));
        respuesta.put("resultados", resultados);
        return respuesta;
    }

    // ---------------- Detalle ----------------

    public Map<String, Object> detalle(long id, String tipo, String pais) {
        if (id <= 0) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "ID de titulo invalido");
        }
        if (!"pelicula".equals(tipo) && !"serie".equals(tipo)) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Tipo invalido (pelicula | serie)");
        }
        String codigoPais = pais == null ? "MX" : pais.trim().toUpperCase();
        if (!codigoPais.matches("^[A-Z]{2}$")) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Pais invalido (codigo de 2 letras)");
        }

        String ruta = "pelicula".equals(tipo) ? "/movie/" + id : "/tv/" + id;
        // Una sola peticion trae detalle + reparto + plataformas
        Map<String, String> params = new LinkedHashMap<>();
        params.put("append_to_response", "credits,watch/providers");
        Map<String, Object> d = pedir(ruta, params);

        List<String> generos = new ArrayList<>();
        for (Map<String, Object> g : lista(d, "genres")) {
            String nombre = texto(g, "name");
            if (nombre != null) generos.add(nombre);
        }

        List<Map<String, Object>> cast = new ArrayList<>(lista(mapa(d, "credits"), "cast"));
        cast.sort(Comparator.comparingInt(c -> entero(c, "order", 999)));
        List<Map<String, Object>> reparto = new ArrayList<>();
        for (Map<String, Object> c : cast.subList(0, Math.min(10, cast.size()))) {
            Map<String, Object> miembro = new LinkedHashMap<>();
            miembro.put("nombre", texto(c, "name"));
            String personaje = texto(c, "character");
            miembro.put("personaje", personaje == null ? "" : personaje);
            miembro.put("imagenPerfil", imagen(texto(c, "profile_path"), "w185"));
            reparto.add(miembro);
        }

        Map<String, Object> respuesta = aTitulo(d, tipo);
        respuesta.put("generos", generos);
        respuesta.put("reparto", reparto);
        respuesta.put("disponibilidad", aDisponibilidad(d, codigoPais));
        return respuesta;
    }

    // ---------------- Tendencias (pantalla de inicio) ----------------

    /** Lo mas visto en el mundo. periodo: "semana" (por defecto) o "dia". Maximo 20 titulos. */
    public Map<String, Object> tendencias(String periodo) {
        boolean dia = "dia".equals(periodo);
        Map<String, Object> datos = pedir("/trending/all/" + (dia ? "day" : "week"), new LinkedHashMap<>());

        List<Map<String, Object>> resultados = new ArrayList<>();
        for (Map<String, Object> r : lista(datos, "results")) {
            String media = texto(r, "media_type");
            if ("movie".equals(media)) {
                resultados.add(aTitulo(r, "pelicula"));
            } else if ("tv".equals(media)) {
                resultados.add(aTitulo(r, "serie"));
            }
            if (resultados.size() == 20) break;
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("periodo", dia ? "dia" : "semana");
        respuesta.put("resultados", resultados);
        return respuesta;
    }

    // ---------------- Internos ----------------

    /** Busca primero en la cache (tabla cache_tmdb); si no esta o ya vencio, llama a TMDB y guarda. */
    private Map<String, Object> pedir(String ruta, Map<String, String> parametros) {
        String clave = clave(ruta, parametros);
        CacheTmdb guardado = buscarEnCache(clave);
        if (guardado != null && guardado.getVenceEn() != null
                && guardado.getVenceEn().isAfter(LocalDateTime.now())) {
            Map<String, Object> copia = leer(guardado.getContenido());
            if (copia != null) return copia;
        }
        try {
            Map<String, Object> fresco = pedirATmdb(ruta, parametros);
            guardarEnCache(guardado, clave, ruta, parametros, fresco);
            return fresco;
        } catch (ArcoException e) {
            // TMDB fallo: si hay una copia (aunque este vencida) se usa, salvo que el titulo no exista
            if (guardado != null && e.getEstado() != HttpStatus.NOT_FOUND) {
                Map<String, Object> copia = leer(guardado.getContenido());
                if (copia != null) return copia;
            }
            throw e;
        }
    }

    private String clave(String ruta, Map<String, String> parametros) {
        StringBuilder sb = new StringBuilder(ruta).append("?language=").append(IDIOMA);
        for (Map.Entry<String, String> p : new TreeMap<>(parametros).entrySet()) {
            sb.append('&').append(p.getKey()).append('=')
                    .append(p.getValue().trim().toLowerCase(Locale.ROOT));
        }
        String texto = sb.toString();
        return texto.length() > 300 ? texto.substring(0, 300) : texto;
    }

    private CacheTmdb buscarEnCache(String clave) {
        try {
            return cache.findByClave(clave).orElse(null);
        } catch (RuntimeException e) {
            return null; // si la base falla, se sigue sin cache
        }
    }

    private void guardarEnCache(CacheTmdb anterior, String clave, String ruta,
                                Map<String, String> parametros, Map<String, Object> datos) {
        try {
            CacheTmdb fila = anterior != null ? anterior : new CacheTmdb();
            LocalDateTime ahora = LocalDateTime.now();
            Duration vigencia;
            String descripcion;
            if (ruta.startsWith("/search")) {
                vigencia = VIGENCIA_BUSQUEDA;
                descripcion = "Busqueda: " + parametros.get("query") + " (pagina " + parametros.get("page") + ")";
            } else if (ruta.startsWith("/trending")) {
                vigencia = VIGENCIA_TENDENCIAS;
                descripcion = ruta.endsWith("day") ? "Tendencias del dia" : "Tendencias de la semana";
            } else {
                vigencia = VIGENCIA_DETALLE;
                String nombre = texto(datos, "title");
                if (nombre == null) nombre = texto(datos, "name");
                descripcion = (ruta.startsWith("/tv") ? "Serie: " : "Pelicula: ") + (nombre == null ? ruta : nombre);
            }
            fila.setClave(clave);
            fila.setDescripcion(descripcion.length() > 300 ? descripcion.substring(0, 300) : descripcion);
            fila.setContenido(escribir(datos));
            fila.setGuardadoEn(ahora);
            fila.setVenceEn(ahora.plus(vigencia));
            cache.save(fila);
        } catch (RuntimeException e) {
            // La cache es opcional: si no se pudo guardar (ej. dos busquedas iguales al mismo tiempo), no pasa nada
        }
    }

    private byte[] escribir(Map<String, Object> datos) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(new LinkedHashMap<>(datos));
            salida.flush();
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar en cache", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leer(byte[] contenido) {
        if (contenido == null) return null;
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(contenido))) {
            entrada.setObjectInputFilter(FILTRO_CACHE);
            Object valor = entrada.readObject();
            return valor instanceof Map<?, ?> ? (Map<String, Object>) valor : null;
        } catch (Exception e) {
            return null; // copia danada: se vuelve a pedir a TMDB
        }
    }

    private Map<String, Object> pedirATmdb(String ruta, Map<String, String> parametros) {
        if (token.isBlank() || token.startsWith("PEGA_AQUI")) {
            throw new ArcoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta el token de TMDB: pegalo en application-local.properties (tmdb.token) y reinicia Spring");
        }
        try {
            Map<String, Object> cuerpo = cliente.get()
                    .uri(b -> {
                        b.path(ruta).queryParam("language", "{language}");
                        for (String clave : parametros.keySet()) {
                            b.queryParam(clave, "{" + clave + "}");
                        }
                        Map<String, Object> valores = new LinkedHashMap<>(parametros);
                        valores.put("language", IDIOMA);
                        return b.build(valores);
                    })
                    .retrieve()
                    .body(TIPO_MAPA);
            if (cuerpo == null) {
                throw new ArcoException(HttpStatus.BAD_GATEWAY, "TMDB respondio vacio");
            }
            return cuerpo;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ArcoException(HttpStatus.NOT_FOUND, "Titulo no encontrado");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ArcoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TMDB rechazo el token. Revisa tmdb.token en application-local.properties");
        } catch (RestClientResponseException e) {
            throw new ArcoException(HttpStatus.BAD_GATEWAY,
                    "TMDB respondio con estado " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ArcoException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con TMDB");
        }
    }

    private Map<String, Object> aTitulo(Map<String, Object> t, String tipo) {
        boolean pelicula = "pelicula".equals(tipo);
        String fecha = texto(t, pelicula ? "release_date" : "first_air_date");
        Integer anio = (fecha != null && fecha.matches("^\\d{4}.*")) ? Integer.valueOf(fecha.substring(0, 4)) : null;
        String nombre = texto(t, pelicula ? "title" : "name");
        String sinopsis = texto(t, "overview");
        Object voto = t.get("vote_average");
        double calificacion = voto instanceof Number n ? Math.round(n.doubleValue() * 10) / 10.0 : 0.0;

        Map<String, Object> titulo = new LinkedHashMap<>();
        titulo.put("tmdbId", t.get("id") instanceof Number n ? n.longValue() : null);
        titulo.put("tipo", tipo);
        titulo.put("nombre", nombre == null ? "Sin titulo" : nombre);
        titulo.put("sinopsis", sinopsis == null ? "" : sinopsis);
        titulo.put("anio", anio);
        titulo.put("calificacion", calificacion);
        titulo.put("imagenPoster", imagen(texto(t, "poster_path"), "w500"));
        titulo.put("imagenFondo", imagen(texto(t, "backdrop_path"), "w1280"));
        return titulo;
    }

    private Map<String, Object> aDisponibilidad(Map<String, Object> d, String pais) {
        Map<String, Object> region = mapa(mapa(mapa(d, "watch/providers"), "results"), pais);
        Map<String, Object> disponibilidad = new LinkedHashMap<>();
        disponibilidad.put("pais", pais);
        disponibilidad.put("suscripcion", plataformas(lista(region, "flatrate")));
        disponibilidad.put("renta", plataformas(lista(region, "rent")));
        disponibilidad.put("compra", plataformas(lista(region, "buy")));
        return disponibilidad;
    }

    private List<Map<String, Object>> plataformas(List<Map<String, Object>> origen) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> p : origen) {
            Map<String, Object> plataforma = new LinkedHashMap<>();
            plataforma.put("nombre", texto(p, "provider_name"));
            plataforma.put("logo", imagen(texto(p, "logo_path"), "w92"));
            salida.add(plataforma);
        }
        return salida;
    }

    private String imagen(String ruta, String tamano) {
        return (ruta == null || ruta.isBlank()) ? null : URL_IMG + "/" + tamano + ruta;
    }

    // Lectura segura del JSON (TMDB a veces manda null o campos que faltan)
    private String texto(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v instanceof String s ? s : null;
    }

    private int entero(Map<String, Object> m, String clave, int porDefecto) {
        Object v = m.get(clave);
        return v instanceof Number n ? n.intValue() : porDefecto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v instanceof Map<?, ?> ? (Map<String, Object>) v : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lista(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v instanceof List<?> ? (List<Map<String, Object>>) v : new ArrayList<>();
    }
}
