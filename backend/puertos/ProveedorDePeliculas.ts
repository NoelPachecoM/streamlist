import type { DetalleTitulo, ResultadoBusqueda, TipoTitulo } from '../dominio/entidades/Titulo';

/** Puerto: de dónde salen los datos de películas y series. */
export interface ProveedorDePeliculas {
  buscar(consulta: string, pagina: number): Promise<ResultadoBusqueda>;
  obtenerDetalle(tmdbId: number, tipo: TipoTitulo, pais: string): Promise<DetalleTitulo>;
}
