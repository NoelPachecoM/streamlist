export type TipoTitulo = 'pelicula' | 'serie';

export interface ResultadoBusqueda {
  tmdbId: number;
  tipo: TipoTitulo;
  nombre: string;
  sinopsis: string;
  anio: number | null;
  calificacion: number | null;
  imagenPoster: string | null;
  imagenFondo: string | null;
}

export interface PaginaResultados {
  pagina: number;
  totalPaginas: number;
  totalResultados: number;
  resultados: ResultadoBusqueda[];
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

export interface DisponibilidadStreaming {
  pais: string;
  suscripcion: PlataformaStreaming[];
  renta: PlataformaStreaming[];
  compra: PlataformaStreaming[];
}

export interface DetalleTitulo extends ResultadoBusqueda {
  generos: string[];
  reparto: MiembroReparto[];
  disponibilidad: DisponibilidadStreaming;
}