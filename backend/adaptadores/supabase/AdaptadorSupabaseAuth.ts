import type { SupabaseClient } from '@supabase/supabase-js';
import { ErrorNoAutenticado, ErrorProveedorExterno } from '../../dominio/errores';
import type { ProveedorDeAutenticacion, UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';

/**
 * Adaptador: implementa ProveedorDeAutenticacion para las cuentas de correo y
 * contraseña creadas con Supabase Auth. Le pregunta a Supabase si el token es
 * válido (firma, expiración y que el usuario siga existiendo), así funciona
 * igual con cualquier tipo de llave de firma del proyecto.
 * El id del usuario es el UUID de auth.users.
 *
 * Usa el mismo cliente de Supabase que ya usa el backend para la lista, por lo
 * que no necesita variables de entorno adicionales.
 */
export class AdaptadorSupabaseAuth implements ProveedorDeAutenticacion {
  /** Valor del claim "iss" de los tokens de este proyecto. */
  readonly emisor: string;

  constructor(
    url: string,
    private readonly cliente: SupabaseClient,
  ) {
    this.emisor = `${url.replace(/\/+$/, '')}/auth/v1`;
  }

  async verificarToken(token: string): Promise<UsuarioAutenticado> {
    let respuesta;
    try {
      respuesta = await this.cliente.auth.getUser(token);
    } catch (error) {
      console.error('[auth] No se pudo contactar a Supabase Auth:', error);
      throw new ErrorProveedorExterno('No se pudo verificar la sesión');
    }

    const { data, error } = respuesta;
    if (error) {
      const estado = (error as { status?: number }).status;
      console.warn('[auth] Supabase no aceptó el token:', estado, error.message);
      // Un 4xx significa token inválido o vencido: el usuario debe iniciar sesión de nuevo (401).
      // Cualquier otra cosa (red, 5xx, límite de peticiones) es un fallo del servicio: se responde 502
      // para que la interfaz NO cierre la sesión del usuario por un problema que no es suyo.
      if (typeof estado === 'number' && estado >= 400 && estado < 500 && estado !== 429) {
        throw new ErrorNoAutenticado();
      }
      throw new ErrorProveedorExterno('No se pudo verificar la sesión');
    }
    if (!data.user) throw new ErrorNoAutenticado();
    return { id: data.user.id, correo: data.user.email ?? undefined };
  }
}
