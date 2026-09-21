import type { TipoTitulo } from './Titulo';

export type EstadoLista = 'por_ver' | 'viendo' | 'vista';

export const ESTADOS_LISTA: readonly EstadoLista[] = ['por_ver', 'viendo', 'vista'];
export const TIPOS_TITULO: readonly TipoTitulo[] = ['pelicula', 'serie'];

export interface ItemLista {
  id: string;
  /** ID del usuario tal como lo entrega Auth0 (ej. "auth0|64f2..."). */
  usuarioId: string;
  tmdbId: number;
  tipo: TipoTitulo;
  estado: EstadoLista;
  calificacion: number | null;
  resena: string | null;
  creadoEn: string;
}
