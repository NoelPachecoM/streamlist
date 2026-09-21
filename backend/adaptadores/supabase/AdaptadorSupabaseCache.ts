import type { SupabaseClient } from '@supabase/supabase-js';
import type { DetalleTitulo, TipoTitulo } from '../../dominio/entidades/Titulo';
import type { CacheDeDetalles } from '../../puertos/CacheDeDetalles';

const TABLA = 'titulos_cache';

interface EntradaPais {
  guardadoEn: string;
  detalle: DetalleTitulo;
}

/** Un mismo título puede guardarse para varios países (las plataformas cambian). */
type DatosPorPais = Record<string, EntradaPais | undefined>;

/**
 * Adaptador: implementa CacheDeDetalles sobre la tabla titulos_cache de Supabase.
 * Guarda el detalle completo en la columna "datos" (jsonb), una entrada por país.
 */
export class AdaptadorSupabaseCache implements CacheDeDetalles {
  /**
   * @param vigenciaMs cuánto tiempo se considera fresco un detalle guardado
   */
  constructor(
    private readonly cliente: SupabaseClient,
    private readonly vigenciaMs: number,
  ) {}

  async obtener(tmdbId: number, tipo: TipoTitulo, pais: string): Promise<DetalleTitulo | null> {
    const datos = await this.leerDatos(tmdbId, tipo);
    const entrada = datos?.[pais];
    if (!entrada) return null;
    const antiguedad = Date.now() - new Date(entrada.guardadoEn).getTime();
    return antiguedad < this.vigenciaMs ? entrada.detalle : null;
  }

  async guardar(detalle: DetalleTitulo, pais: string): Promise<void> {
    const existentes = (await this.leerDatos(detalle.tmdbId, detalle.tipo)) ?? {};
    const datos: DatosPorPais = {
      ...existentes,
      [pais]: { guardadoEn: new Date().toISOString(), detalle },
    };
    const { error } = await this.cliente
      .from(TABLA)
      .upsert(
        {
          tmdb_id: detalle.tmdbId,
          tipo: detalle.tipo,
          nombre: detalle.nombre,
          imagen_poster: detalle.imagenPoster,
          imagen_fondo: detalle.imagenFondo,
          sinopsis: detalle.sinopsis,
          datos,
        },
                { onConflict: 'tmdb_id,tipo' },
      );
    if (error) throw new Error(error.message);
  }

  private async leerDatos(tmdbId: number, tipo: TipoTitulo): Promise<DatosPorPais | null> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .select('datos')
      .eq('tmdb_id', tmdbId)
      .eq('tipo', tipo)
      .maybeSingle();
    if (error) throw new Error(error.message);
    return (data?.datos as DatosPorPais | null) ?? null;
  }
}