import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';
import { ErrorNoAutenticado } from '../../dominio/errores';
import type { ProveedorDeAutenticacion, UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';

/**
 * Adaptador: implementa ProveedorDeAutenticacion para las cuentas de correo y
 * contraseña creadas con Supabase Auth. Le pregunta a Supabase si el token es
 * válido (firma, expiración y que el usuario siga existiendo), así funciona
 * igual con cualquier tipo de llave de firma del proyecto.
 * El id del usuario es el UUID de auth.users.
 */
export class AdaptadorSupabaseAuth implements ProveedorDeAutenticacion {
  /** Valor del claim "iss" de los tokens de este proyecto. */
  readonly emisor: string;
  private readonly cliente: SupabaseClient;

  /** @param cliente solo para pruebas: permite inyectar un cliente falso */
  constructor(url: string, anonKey: string, cliente?: SupabaseClient) {
    this.emisor = `${url.replace(/\/+$/, '')}/auth/v1`;
    this.cliente =
      cliente ?? createClient(url, anonKey, { auth: { persistSession: false, autoRefreshToken: false } });
  }

  async verificarToken(token: string): Promise<UsuarioAutenticado> {
    try {
      const { data, error } = await this.cliente.auth.getUser(token);
      if (error || !data.user) throw new ErrorNoAutenticado();
      return { id: data.user.id, correo: data.user.email ?? undefined };
    } catch (error) {
      if (error instanceof ErrorNoAutenticado) throw error;
      throw new ErrorNoAutenticado();
    }
  }
}
