import { decodeJwt } from 'jose';
import { ErrorNoAutenticado } from '../dominio/errores';
import type { ProveedorDeAutenticacion, UsuarioAutenticado } from '../puertos/ProveedorDeAutenticacion';

export interface ProveedorConEmisores {
  /** Valores de "iss" que este proveedor sabe verificar. */
  emisores: string[];
  proveedor: ProveedorDeAutenticacion;
}

/**
 * Acepta varios métodos de acceso a la vez (Google, Supabase...). Mira el emisor
 * ("iss") del token y delega la verificación REAL (firma, audiencia, expiración)
 * al proveedor correspondiente. Leer el "iss" aquí solo sirve para elegir a quién
 * preguntar: nunca se confía en él sin que el proveedor verifique el token.
 */
export class ProveedorDeAutenticacionCompuesto implements ProveedorDeAutenticacion {
  constructor(private readonly proveedores: ProveedorConEmisores[]) {}

  async verificarToken(token: string): Promise<UsuarioAutenticado> {
    let emisor: string | undefined;
    try {
      emisor = decodeJwt(token).iss;
    } catch {
      throw new ErrorNoAutenticado();
    }
    const elegido = this.proveedores.find((p) => emisor !== undefined && p.emisores.includes(emisor));
    if (!elegido) throw new ErrorNoAutenticado();
    return elegido.proveedor.verificarToken(token);
  }
}
