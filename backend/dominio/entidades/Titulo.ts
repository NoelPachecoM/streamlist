export type TipoTitulo = 'pelicula' | 'serie';

export interface Titulo {
  tmdbId: number;
  tipo: TipoTitulo;
  nombre: string;
  sinopsis: string;
  anio: number | null;
  /** Promedio de TMDB, de 0 a 10. */
  calificacion: number;
  imagenPoster: string | null;
  imagenFondo: string | null;
}

export interface MiembroReparto {
  nombre: string;
  personaje: string;
  imagenPerfil: string | null;
}

export interface PlataformaStreaming {
  nombre: string;
  logo: string | null;
}

/** Dónde ver un título en un país concreto. */
export interface DisponibilidadStreaming {
  pais: string;
  suscripcion: PlataformaStreaming[];
  renta: PlataformaStreaming[];
  compra: PlataformaStreaming[];
}

export interface DetalleTitulo extends Titulo {
  generos: string[];
  reparto: MiembroReparto[];
  disponibilidad: DisponibilidadStreaming;
}

export interface ResultadoBusqueda {
  pagina: number;
  totalPaginas: number;
  totalResultados: number;
  resultados: Titulo[];
}
