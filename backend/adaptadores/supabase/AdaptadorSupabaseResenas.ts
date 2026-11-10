import type { SupabaseClient } from '@supabase/supabase-js';
import { ErrorDeAlmacenamiento } from '../../dominio/errores';
import { datosDePerfiles, esUuid } from './ayudasPerfiles';

export interface ResenaAdmin {
  id: string;
  usuarioId: string;
  usuarioNombre: string;
  usuarioCorreo: string | null;
  tmdbId: number;
  tipo: string;
  titulo: string;
  calificacion: number | null;
  resena: string;
  creadoEn: string;
  actualizadoEn: string;
}

interface FilaResena {
  id: string;
  usuario_id: string;
  tmdb_id: number;
  tipo: string;
  calificacion: number | null;
  resena: string | null;
  creado_en: string;
  actualizado_en: string;
}

interface FilaTitulo {
  tmdb_id: number;
  tipo: string;
  nombre: string;
}

const TABLA = 'lista_usuario';
const CAMPOS = 'id,usuario_id,tmdb_id,tipo,calificacion,resena,creado_en,actualizado_en';
const LIMITE = 500;

export class AdaptadorSupabaseResenas {
  constructor(private readonly cliente: SupabaseClient) {}

  async listar(): Promise<ResenaAdmin[]> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .select(CAMPOS)
      .not('resena', 'is', null)
      .neq('resena', '')
      .order('actualizado_en', { ascending: false })
      .limit(LIMITE);
    if (error) throw new ErrorDeAlmacenamiento();
    return this.enriquecer(data as FilaResena[]);
  }

  async obtener(id: string): Promise<ResenaAdmin | null> {
    const { data, error } = await this.cliente.from(TABLA).select(CAMPOS).eq('id', id).maybeSingle();
    if (error) throw new ErrorDeAlmacenamiento();
    const fila = data as FilaResena | null;
    if (!fila?.resena) return null;
    return (await this.enriquecer([fila]))[0] ?? null;
  }

  /**
   * Borra solo el texto de la reseña (moderación); conserva la calificación y el estado.
   * Devuelve la reseña tal como estaba, para poder registrarla en la bitácora.
   */
  async quitarTexto(id: string): Promise<ResenaAdmin | null> {
    const previa = await this.obtener(id);
    if (!previa) return null;
    const { error } = await this.cliente.from(TABLA).update({ resena: null }).eq('id', id);
    if (error) throw new ErrorDeAlmacenamiento();
    return previa;
  }

  private async enriquecer(filas: FilaResena[]): Promise<ResenaAdmin[]> {
    if (filas.length === 0) return [];
    const perfiles = await datosDePerfiles(this.cliente, filas.map((fila) => fila.usuario_id));
    const titulos = await this.titulos(filas);

    return filas.map((fila) => {
      const perfil = perfiles.get(fila.usuario_id);
      return {
        id: fila.id,
        usuarioId: fila.usuario_id,
        usuarioNombre: perfil?.nombre ?? (esUuid(fila.usuario_id) ? 'Cuenta sin perfil' : 'Cuenta de Google'),
        usuarioCorreo: perfil?.correo ?? null,
        tmdbId: fila.tmdb_id,
        tipo: fila.tipo,
        titulo: titulos.get(`${fila.tipo}:${fila.tmdb_id}`) ?? `Título #${fila.tmdb_id}`,
        calificacion: fila.calificacion,
        resena: fila.resena ?? '',
        creadoEn: fila.creado_en,
        actualizadoEn: fila.actualizado_en,
      };
    });
  }

  private async titulos(filas: FilaResena[]): Promise<Map<string, string>> {
    const mapa = new Map<string, string>();
    const ids = [...new Set(filas.map((fila) => fila.tmdb_id))];
    const { data, error } = await this.cliente
      .from('titulos_cache')
      .select('tmdb_id,tipo,nombre')
      .in('tmdb_id', ids);
    if (error) throw new ErrorDeAlmacenamiento();
    for (const fila of data as FilaTitulo[]) mapa.set(`${fila.tipo}:${fila.tmdb_id}`, fila.nombre);
    return mapa;
  }
}
