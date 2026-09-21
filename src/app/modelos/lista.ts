import { TipoTitulo } from './titulo';

export type EstadoLista = 'por_ver' | 'viendo' | 'vista';

export const ESTADOS: { valor: EstadoLista; etiqueta: string }[] = [
  { valor: 'por_ver', etiqueta: 'Por ver' },
  { valor: 'viendo', etiqueta: 'Viendo' },
  { valor: 'vista', etiqueta: 'Vista' },
];

export interface ItemLista {
  id: string;
  usuarioId: string;
  tmdbId: number;
  tipo: TipoTitulo;
  estado: EstadoLista;
  calificacion: number | null;
  resena: string | null;
  creadoEn: string;
}