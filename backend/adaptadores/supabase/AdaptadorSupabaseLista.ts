import type { SupabaseClient } from '@supabase/supabase-js';
import type { EstadoLista, ItemLista } from '../../dominio/entidades/ItemLista';
import type { TipoTitulo } from '../../dominio/entidades/Titulo';
import { ErrorDeAlmacenamiento, ErrorItemDuplicado, ErrorNoEncontrado } from '../../dominio/errores';
import type { CambiosItemLista, NuevoItemLista, RepositorioDeLista } from '../../puertos/RepositorioDeLista';

const TABLA = 'lista_usuario';

interface FilaLista {
  id: string;
  usuario_id: string;
  tmdb_id: number;
  tipo: TipoTitulo;
  estado: EstadoLista;
  calificacion: number | null;
  resena: string | null;
  creado_en: string;
}

/**
 * Adaptador: implementa RepositorioDeLista sobre Supabase (Postgres).
 * Se usa con la llave service_role, así que CADA consulta se filtra por usuario_id
 * (el ID que viene del token ya verificado). Esa es la barrera de privacidad.
 */
export class AdaptadorSupabaseLista implements RepositorioDeLista {
  constructor(private readonly cliente: SupabaseClient) {}

  async agregar(item: NuevoItemLista): Promise<ItemLista> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .insert({ usuario_id: item.usuarioId, tmdb_id: item.tmdbId, tipo: item.tipo, estado: item.estado })
      .select()
      .single();

    if (error) {
      if (error.code === '23505') throw new ErrorItemDuplicado();
      throw new ErrorDeAlmacenamiento();
    }
    return this.aItem(data as FilaLista);
  }

  async obtenerPorUsuario(usuarioId: string, estado?: EstadoLista): Promise<ItemLista[]> {
    let consulta = this.cliente.from(TABLA).select().eq('usuario_id', usuarioId);
    if (estado) consulta = consulta.eq('estado', estado);
    const { data, error } = await consulta.order('creado_en', { ascending: false });
    if (error) throw new ErrorDeAlmacenamiento();
    return (data as FilaLista[]).map((f) => this.aItem(f));
  }

  async actualizar(usuarioId: string, idItem: string, cambios: CambiosItemLista): Promise<ItemLista> {
    const parche: Record<string, unknown> = {};
    if (cambios.estado !== undefined) parche['estado'] = cambios.estado;
    if (cambios.calificacion !== undefined) parche['calificacion'] = cambios.calificacion;
    if (cambios.resena !== undefined) parche['resena'] = cambios.resena;

    const { data, error } = await this.cliente
      .from(TABLA)
      .update(parche)
      .eq('id', idItem)
      .eq('usuario_id', usuarioId)
      .select()
      .maybeSingle();

    if (error) throw new ErrorDeAlmacenamiento();
    if (!data) throw new ErrorNoEncontrado('Elemento no encontrado en tu lista');
    return this.aItem(data as FilaLista);
  }

  async eliminar(usuarioId: string, idItem: string): Promise<void> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .delete()
      .eq('id', idItem)
      .eq('usuario_id', usuarioId)
      .select('id');

    if (error) throw new ErrorDeAlmacenamiento();
    if (!data || data.length === 0) throw new ErrorNoEncontrado('Elemento no encontrado en tu lista');
  }

  private aItem(f: FilaLista): ItemLista {
    return {
      id: f.id,
      usuarioId: f.usuario_id,
      tmdbId: f.tmdb_id,
      tipo: f.tipo,
      estado: f.estado,
      calificacion: f.calificacion,
      resena: f.resena,
      creadoEn: f.creado_en,
    };
  }
}
