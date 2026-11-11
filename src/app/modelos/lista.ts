import { TipoTitulo } from './titulo';

/**
 * Estados que usa la página. Son claves de la tabla estados_lista de Spring:
 * «pendiente» se muestra como «Por ver».
 */
export type EstadoLista = 'pendiente' | 'viendo' | 'completado';

export const ESTADOS: { valor: EstadoLista; etiqueta: string }[] = [
  { valor: 'pendiente', etiqueta: 'Por ver' },
  { valor: 'viendo', etiqueta: 'Viendo' },
  { valor: 'completado', etiqueta: 'Completado' },
];

/** Solo se puede calificar y reseñar cuando el título está en «Completado». */
export function permiteResena(estado: EstadoLista): boolean {
  return estado === 'completado';
}

export function etiquetaEstado(estado: string | null): string {
  return ESTADOS.find((e) => e.valor === estado)?.etiqueta ?? '';
}

/** Un título en Mi lista (respuesta de GET /api/mi-lista). */
export interface ItemLista {
  id: number;
  usuarioId: number;
  tituloId: number;
  tmdbId: number;
  tipo: TipoTitulo;
  nombre: string;
  anio: number | null;
  imagenPoster: string | null;
  estado: EstadoLista;
  calificacion: number | null;
  resena: string | null;
  creadoEn: string | null;
}

/** Una reseña de cualquier usuario (GET /api/mi-lista/resenas). */
export interface Resena {
  id: number;
  usuario: string;
  calificacion: number | null;
  resena: string | null;
  estado: EstadoLista | null;
  fecha: string | null;
}
