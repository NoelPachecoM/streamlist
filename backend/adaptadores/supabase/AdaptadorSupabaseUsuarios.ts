import type { SupabaseClient } from '@supabase/supabase-js';
import type { UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';
import { ErrorDeAlmacenamiento } from '../../dominio/errores';

export type RolId = 1 | 2;

export interface UsuarioAplicacion {
  id: string;
  nombre: string;
  correo: string | null;
  rolId: RolId;
}

interface FilaUsuario {
  id: string;
  nombre: string;
  correo: string | null;
  rol_id: RolId;
}

const TABLA = 'perfiles';
const CAMPOS = 'id,nombre,correo,rol_id';

export class AdaptadorSupabaseUsuarios {
  constructor(private readonly cliente: SupabaseClient) {}

  /** Devuelve el perfil existente; solo crea la fila si todavía no existe. */
  async guardarPerfil(usuario: UsuarioAutenticado): Promise<UsuarioAplicacion> {
    const existente = await this.obtenerPorId(usuario.id);
    if (existente) return existente;

    const { data, error } = await this.cliente
      .from(TABLA)
      .insert({
        id: usuario.id,
        nombre: usuario.nombre ?? usuario.correo ?? 'Usuario',
        correo: usuario.correo ?? null,
      })
      .select(CAMPOS)
      .single();

    if (error) {
      // Otra petición pudo crearlo al mismo tiempo.
      const creado = await this.obtenerPorId(usuario.id);
      if (creado) return creado;
      throw new ErrorDeAlmacenamiento();
    }
    return this.aUsuario(data as FilaUsuario);
  }

  async obtenerPorId(id: string): Promise<UsuarioAplicacion | null> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .select(CAMPOS)
      .eq('id', id)
      .maybeSingle();

    if (error) throw new ErrorDeAlmacenamiento();
    return data ? this.aUsuario(data as FilaUsuario) : null;
  }

  async obtenerTodos(): Promise<UsuarioAplicacion[]> {
    const { data, error } = await this.cliente.from(TABLA).select(CAMPOS).order('nombre');
    if (error) throw new ErrorDeAlmacenamiento();
    return (data as FilaUsuario[]).map((fila) => this.aUsuario(fila));
  }

  async cambiarRol(id: string, rolId: RolId): Promise<UsuarioAplicacion | null> {
    const { data, error } = await this.cliente
      .from(TABLA)
      .update({ rol_id: rolId })
      .eq('id', id)
      .select(CAMPOS)
      .maybeSingle();

    if (error) throw new ErrorDeAlmacenamiento();
    return data ? this.aUsuario(data as FilaUsuario) : null;
  }

  private aUsuario(fila: FilaUsuario): UsuarioAplicacion {
    return { id: fila.id, nombre: fila.nombre, correo: fila.correo, rolId: fila.rol_id };
  }
}
