import type {
  DetalleTitulo,
  DisponibilidadStreaming,
  PlataformaStreaming,
  ResultadoBusqueda,
  TipoTitulo,
  Titulo,
} from '../../dominio/entidades/Titulo';
import { ErrorNoEncontrado, ErrorProveedorExterno } from '../../dominio/errores';
import type { ProveedorDePeliculas } from '../../puertos/ProveedorDePeliculas';

const URL_API = 'https://api.themoviedb.org/3';
const URL_IMG = 'https://image.tmdb.org/t/p';

// --- Formas parciales de las respuestas de TMDB (solo lo que usamos) ---
interface TmdbItem {
  id: number;
  media_type?: string;
  title?: string;
  name?: string;
  overview?: string;
  release_date?: string;
  first_air_date?: string;
  vote_average?: number;
  poster_path?: string | null;
  backdrop_path?: string | null;
}
interface TmdbBusqueda {
  page: number;
  total_pages: number;
  total_results: number;
  results: TmdbItem[];
}
interface TmdbPlataforma {
  provider_name: string;
  logo_path?: string | null;
}
interface TmdbDetalle extends TmdbItem {
  genres?: { name: string }[];
  credits?: {
    cast?: { name: string; character?: string; profile_path?: string | null; order?: number }[];
  };
  'watch/providers'?: {
    results?: Record<string, { flatrate?: TmdbPlataforma[]; rent?: TmdbPlataforma[]; buy?: TmdbPlataforma[] }>;
  };
}

/** Adaptador: implementa el puerto ProveedorDePeliculas usando la API de TMDB. */
export class AdaptadorTMDB implements ProveedorDePeliculas {
  constructor(
    private readonly tokenLectura: string,
    private readonly idioma = 'es-MX',
    private readonly fetchFn: typeof fetch = fetch,
  ) {}

  async buscar(consulta: string, pagina: number): Promise<ResultadoBusqueda> {
    const datos = await this.peticion<TmdbBusqueda>('/search/multi', {
      query: consulta,
      page: String(pagina),
      include_adult: 'false',
    });

    const resultados = datos.results
      .filter((r) => r.media_type === 'movie' || r.media_type === 'tv')
      .map((r) => this.aTitulo(r, r.media_type === 'movie' ? 'pelicula' : 'serie'));

    return {
      pagina: datos.page,
      totalPaginas: datos.total_pages,
      totalResultados: datos.total_results,
      resultados,
    };
  }

  async obtenerDetalle(tmdbId: number, tipo: TipoTitulo, pais: string): Promise<DetalleTitulo> {
    const ruta = tipo === 'pelicula' ? `/movie/${tmdbId}` : `/tv/${tmdbId}`;
    // Una sola petición trae detalle + reparto + plataformas (ahorra cuota).
    const d = await this.peticion<TmdbDetalle>(ruta, { append_to_response: 'credits,watch/providers' });

    const reparto = (d.credits?.cast ?? [])
      .sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
      .slice(0, 10)
      .map((c) => ({
        nombre: c.name,
        personaje: c.character ?? '',
        imagenPerfil: this.imagen(c.profile_path, 'w185'),
      }));

    return {
      ...this.aTitulo(d, tipo),
      generos: (d.genres ?? []).map((g) => g.name),
      reparto,
      disponibilidad: this.aDisponibilidad(d, pais),
    };
  }

  // ---------------- internos ----------------

  private async peticion<T>(ruta: string, parametros: Record<string, string>): Promise<T> {
    const url = new URL(URL_API + ruta);
    url.searchParams.set('language', this.idioma);
    for (const [clave, valor] of Object.entries(parametros)) url.searchParams.set(clave, valor);

    let respuesta: Response;
    try {
      respuesta = await this.fetchFn(url, {
        headers: { Authorization: `Bearer ${this.tokenLectura}`, accept: 'application/json' },
        signal: AbortSignal.timeout(8000),
      });
    } catch {
      throw new ErrorProveedorExterno('No se pudo conectar con TMDB');
    }

    if (respuesta.status === 404) throw new ErrorNoEncontrado('Título no encontrado');
    if (!respuesta.ok) throw new ErrorProveedorExterno(`TMDB respondió con estado ${respuesta.status}`);
    return (await respuesta.json()) as T;
  }

  private aTitulo(t: TmdbItem, tipo: TipoTitulo): Titulo {
    const fecha = tipo === 'pelicula' ? t.release_date : t.first_air_date;
    const anio = fecha && /^\d{4}/.test(fecha) ? Number(fecha.slice(0, 4)) : null;
    return {
      tmdbId: t.id,
      tipo,
      nombre: (tipo === 'pelicula' ? t.title : t.name) ?? 'Sin título',
      sinopsis: t.overview ?? '',
      anio,
      calificacion: Math.round((t.vote_average ?? 0) * 10) / 10,
      imagenPoster: this.imagen(t.poster_path, 'w500'),
      imagenFondo: this.imagen(t.backdrop_path, 'w1280'),
    };
  }

  private aDisponibilidad(d: TmdbDetalle, pais: string): DisponibilidadStreaming {
    const region = d['watch/providers']?.results?.[pais];
    const mapear = (lista?: TmdbPlataforma[]): PlataformaStreaming[] =>
      (lista ?? []).map((p) => ({ nombre: p.provider_name, logo: this.imagen(p.logo_path, 'w92') }));
    return {
      pais,
      suscripcion: mapear(region?.flatrate),
      renta: mapear(region?.rent),
      compra: mapear(region?.buy),
    };
  }

  private imagen(ruta: string | null | undefined, tamano: string): string | null {
    return ruta ? `${URL_IMG}/${tamano}${ruta}` : null;
  }
}
