import type { DetalleTitulo, TipoTitulo } from '../dominio/entidades/Titulo';

/** Puerto: almacén temporal de detalles de títulos ya consultados. */
export interface CacheDeDetalles {
  /** Devuelve el detalle guardado, o null si no existe o ya venció. */
  obtener(tmdbId: number, tipo: TipoTitulo, pais: string): Promise<DetalleTitulo | null>;
  guardar(detalle: DetalleTitulo, pais: string): Promise<void>;
}