import type { SupabaseClient } from '@supabase/supabase-js';
import { ErrorDeAlmacenamiento } from '../../dominio/errores';
import { datosDePerfiles } from './ayudasPerfiles';

export interface EntradaBitacora {
  adminId: string;
  accion: string;
  entidad: string;
  entidadId?: string | null;
  detalle?: Record<string, unknown> | null;
}

export interface RegistroBitacora {
  id: number;
  adminId: string;
  adminNombre: string;
  accion: string;
  entidad: string;
  entidadId: string | null;
  detalle: Record<string, unknown> | null;
  creadoEn: string;
}

interface FilaBitacora {
  id: number;
  admin_id: string;
  accion: string;
  entidad: string;
  entidad_id: string | null;
  detalle: Record<string, unknown> | null;
  creado_en: string;
}

const TABLA = 'bitacora';
const CAMPOS = 'id,admin_id,accion,entidad,entidad_id,detalle,creado_en';

export class AdaptadorSupabaseBitacora {
  constructor(private readonly cliente: SupabaseClient) {}

  async registrar(entrada: EntradaBitacora): Promise<void> {
    const { error } = await this.cliente.from(TABLA).insert({
      admin_id: entrada.adminId,
      accion: entrada.accion,
      entidad: entrada.entidad,
      entidad_id: entrada.entidadId ?? null,
      detalle: entrada.detalle ?? null,
    });
    if (error) throw new ErrorDeAlmacenamiento();
  }

  async listar(limite = 200): Promise<RegistroBitacora[]> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .select(CAMPOS)
      .order('creado_en', { ascending: false })
      .limit(limite);
    if (error) throw new ErrorDeAlmacenamiento();

    const filas = data as FilaBitacora[];
    const perfiles = await datosDePerfiles(this.cliente, filas.map((fila) => fila.admin_id));
    return filas.map((fila) => ({
      id: fila.id,
      adminId: fila.admin_id,
      adminNombre: perfiles.get(fila.admin_id)?.nombre ?? 'Administrador desconocido',
      accion: fila.accion,
      entidad: fila.entidad,
      entidadId: fila.entidad_id,
      detalle: fila.detalle,
      creadoEn: fila.creado_en,
    }));
  }
}
