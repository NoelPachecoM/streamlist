import type { EstadoLista, ItemLista } from '../dominio/entidades/ItemLista';
import type { TipoTitulo } from '../dominio/entidades/Titulo';

export interface NuevoItemLista {
  usuarioId: string;
  tmdbId: number;
  tipo: TipoTitulo;
  estado: EstadoLista;
}

export interface CambiosItemLista {
  estado?: EstadoLista;
  calificacion?: number | null;
  resena?: string | null;
}

/** Puerto: dónde se guarda la lista personal. Todas las operaciones se acotan al usuario. */
export interface RepositorioDeLista {
  agregar(item: NuevoItemLista): Promise<ItemLista>;
  obtenerPorUsuario(usuarioId: string, estado?: EstadoLista): Promise<ItemLista[]>;
  actualizar(usuarioId: string, idItem: string, cambios: CambiosItemLista): Promise<ItemLista>;
  eliminar(usuarioId: string, idItem: string): Promise<void>;
}
