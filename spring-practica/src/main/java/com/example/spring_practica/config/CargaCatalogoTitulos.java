package com.example.spring_practica.config;

import com.example.spring_practica.modelo.*;
import com.example.spring_practica.repositorio.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Datos de ejemplo de titulos, actores y sus relaciones. Cada tabla se llena solo si esta vacia.
 * Los ids de tipo, genero, estado, plataforma y usuario son de ejemplo (1, 2, 3...): apuntan a
 * lo que CargaDatos ya cargo en esas tablas.
 */
@Component
public class CargaCatalogoTitulos implements CommandLineRunner {

    private final TituloRepositorio titulos;
    private final ActorRepositorio actores;
    private final TituloGeneroRepositorio tituloGeneros;
    private final TituloPaisRepositorio tituloPaises;
    private final DisponibilidadRepositorio disponibilidades;
    private final RepartoRepositorio reparto;
    private final ListaUsuarioRepositorio lista;
    private final FavoritoRepositorio favoritos;
    private final PaisRepositorio paises;
    private final TipoDisponibilidadRepositorio tipos;
    private final CargaPaises cargaPaises;
    private final CargaTiposDisponibilidad cargaTipos;

    public CargaCatalogoTitulos(TituloRepositorio titulos, ActorRepositorio actores,
                                TituloGeneroRepositorio tituloGeneros, TituloPaisRepositorio tituloPaises,
                                DisponibilidadRepositorio disponibilidades, RepartoRepositorio reparto,
                                ListaUsuarioRepositorio lista, FavoritoRepositorio favoritos,
                                PaisRepositorio paises, TipoDisponibilidadRepositorio tipos,
                                CargaPaises cargaPaises, CargaTiposDisponibilidad cargaTipos) {
        this.titulos = titulos;
        this.actores = actores;
        this.tituloGeneros = tituloGeneros;
        this.tituloPaises = tituloPaises;
        this.disponibilidades = disponibilidades;
        this.reparto = reparto;
        this.lista = lista;
        this.favoritos = favoritos;
        this.paises = paises;
        this.tipos = tipos;
        this.cargaPaises = cargaPaises;
        this.cargaTipos = cargaTipos;
    }

    @Override
    public void run(String... args) {
        // Asegura que paises y tipos de disponibilidad ya esten cargados (no hace nada si ya lo estan).
        cargaPaises.run();
        cargaTipos.run();

        if (titulos.count() == 0) {
            titulos.saveAll(List.of(
                    titulo(27205, 1, "Origen", "Un ladron que roba secretos entrando a los suenos recibe una ultima mision.", 2010, 8.4),
                    titulo(155, 1, "El caballero de la noche", "Batman enfrenta al Joker en una guerra por el alma de Gotham.", 2008, 8.5),
                    titulo(1396, 2, "Breaking Bad", "Un profesor de quimica con cancer empieza a fabricar metanfetamina.", 2008, 8.9),
                    titulo(66732, 2, "Stranger Things", "Un grupo de ninos descubre fuerzas sobrenaturales en su pueblo.", 2016, 8.6),
                    titulo(354912, 1, "Coco", "Un nino viaja a la Tierra de los Muertos para descubrir la historia de su familia.", 2017, 8.2),
                    titulo(70523, 2, "Dark", "La desaparicion de un nino destapa secretos de cuatro familias a traves del tiempo.", 2017, 8.4)));
            System.out.println("Carga de titulos terminada.");
        }
        List<Titulo> t = titulos.findAll(Sort.by("id"));

        if (actores.count() == 0) {
            actores.saveAll(List.of(
                    new Actor(null, "Leonardo DiCaprio", "Los Angeles, EE.UU.", "5551230001", "Masculino", "Estados Unidos"),
                    new Actor(null, "Christian Bale", "Los Angeles, EE.UU.", "5551230002", "Masculino", "Reino Unido"),
                    new Actor(null, "Bryan Cranston", "Los Angeles, EE.UU.", "5551230003", "Masculino", "Estados Unidos"),
                    new Actor(null, "Millie Bobby Brown", "Atlanta, EE.UU.", "5551230004", "Femenino", "Reino Unido"),
                    new Actor(null, "Gael Garcia Bernal", "Ciudad de Mexico, Mexico", "5551230005", "Masculino", "Mexico")));
            System.out.println("Carga de actores terminada.");
        }
        List<Actor> a = actores.findAll(Sort.by("idActor"));

        if (tituloGeneros.count() == 0 && t.size() >= 6) {
            tituloGeneros.saveAll(List.of(
                    new TituloGenero(null, t.get(0), 1),
                    new TituloGenero(null, t.get(0), 2),
                    new TituloGenero(null, t.get(1), 1),
                    new TituloGenero(null, t.get(2), 2),
                    new TituloGenero(null, t.get(3), 3),
                    new TituloGenero(null, t.get(4), 1),
                    new TituloGenero(null, t.get(5), 3)));
            System.out.println("Carga de titulo_genero terminada.");
        }

        Pais mx = paises.findByCodigo("MX").orElse(null);
        Pais us = paises.findByCodigo("US").orElse(null);
        TipoDisponibilidad suscripcion = tipos.findByClave("suscripcion").orElse(null);
        TipoDisponibilidad renta = tipos.findByClave("renta").orElse(null);

        if (tituloPaises.count() == 0 && mx != null && us != null && t.size() >= 6) {
            for (int i = 0; i < 6; i++) {
                tituloPaises.save(new TituloPais(null, t.get(i), mx, null));
                tituloPaises.save(new TituloPais(null, t.get(i), us, null));
            }
            System.out.println("Carga de titulo_pais terminada.");
        }

        if (disponibilidades.count() == 0 && suscripcion != null && renta != null) {
            List<TituloPais> tp = tituloPaises.findAll(Sort.by("id"));
            int n = 0;
            for (TituloPais x : tp) {
                disponibilidades.save(new Disponibilidad(null, x, 1, suscripcion));
                if (n % 2 == 0) {
                    disponibilidades.save(new Disponibilidad(null, x, 2, renta));
                }
                n++;
            }
            if (!tp.isEmpty()) System.out.println("Carga de disponibilidad terminada.");
        }

        if (reparto.count() == 0 && t.size() >= 5 && a.size() >= 5) {
            reparto.saveAll(List.of(
                    new Reparto(null, t.get(0), null, 1, "Cobb", a.get(0)),
                    new Reparto(null, t.get(1), null, 1, "Batman", a.get(1)),
                    new Reparto(null, t.get(2), null, 1, "Walter White", a.get(2)),
                    new Reparto(null, t.get(3), null, 1, "Eleven", a.get(3)),
                    new Reparto(null, t.get(4), null, 1, "Hector", a.get(4))));
            System.out.println("Carga de reparto terminada.");
        }

        if (lista.count() == 0 && t.size() >= 3) {
            lista.saveAll(List.of(
                    listaItem(1, t.get(0), 1, 9, "Muy buena, el final me dejo pensando."),
                    listaItem(1, t.get(1), 2, null, null),
                    listaItem(1, t.get(2), 1, 10, "De lo mejor que he visto.")));
            System.out.println("Carga de lista_usuario terminada.");
        }

        if (favoritos.count() == 0 && t.size() >= 3) {
            favoritos.saveAll(List.of(
                    favorito(1, t.get(0)),
                    favorito(1, t.get(2))));
            System.out.println("Carga de favoritos terminada.");
        }
    }

    private Titulo titulo(Integer tmdbId, Integer tipoId, String nombre, String sinopsis, Integer anio, Double calif) {
        Titulo x = new Titulo();
        x.setTmdbId(tmdbId);
        x.setTipoId(tipoId);
        x.setNombre(nombre);
        x.setSinopsis(sinopsis);
        x.setAnio(anio);
        x.setCalificacionTmdb(calif);
        return x;
    }

    private ListaUsuario listaItem(Integer usuarioId, Titulo titulo, Integer estadoId, Integer calif, String resena) {
        ListaUsuario x = new ListaUsuario();
        x.setUsuarioId(usuarioId);
        x.setTitulo(titulo);
        x.setEstadoId(estadoId);
        x.setCalificacion(calif);
        x.setResena(resena);
        return x;
    }

    private Favorito favorito(Integer usuarioId, Titulo titulo) {
        Favorito x = new Favorito();
        x.setUsuarioId(usuarioId);
        x.setTitulo(titulo);
        return x;
    }
}
