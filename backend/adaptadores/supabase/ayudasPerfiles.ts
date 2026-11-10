import type { SupabaseClient } from '@supabase/supabase-js';
import { ErrorDeAlmacenamiento } from '../../dominio/errores';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function esUuid(valor: string): boolean {
  return UUID.test(valor);
}

export interface DatosPerfil {
  nombre: string;
  correo: string | null;
}

interface FilaPerfil {
  id: string;
  nombre: string | null;
  apellidos: string | null;
  correo: string | null;
}

/**
 * Busca nombre y correo de varios perfiles. Ignora los ids que no son uuid
 * (por ejemplo los de Google), porque la columna perfiles.id es uuid y
 * consultarla con otro formato daría error.
 */
export async function datosDePerfiles(
  cliente: SupabaseClient,
  ids: string[],
): Promise<Map<string, DatosPerfil>> {
  const mapa = new Map<string, DatosPerfil>();
  const validos = [...new Set(ids.filter(esUuid))];
  if (validos.length === 0) return mapa;

  const { data, error } = await cliente
    .from('perfiles')
    .select('id,nombre,apellidos,correo')
    .in('id', validos);
  if (error) throw new ErrorDeAlmacenamiento();

  for (const fila of data as FilaPerfil[]) {
    const nombre = [fila.nombre, fila.apellidos].filter(Boolean).join(' ').trim();
    mapa.set(fila.id, { nombre: nombre || fila.correo || 'Sin nombre', correo: fila.correo });
  }
  return mapa;
}
