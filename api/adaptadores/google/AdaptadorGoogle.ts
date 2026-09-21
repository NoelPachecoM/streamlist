import { createRemoteJWKSet, jwtVerify } from 'jose';
import type { JWTVerifyGetKey } from 'jose';
import { ErrorNoAutenticado } from '../../dominio/errores';
import type { ProveedorDeAutenticacion, UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';

const EMISORES_GOOGLE = ['https://accounts.google.com', 'accounts.google.com'];
const URL_CLAVES_GOOGLE = 'https://www.googleapis.com/oauth2/v3/certs';

/**
 * Adaptador: implementa ProveedorDeAutenticacion validando el ID token que
 * Google entrega al usuario cuando inicia sesión con "Iniciar sesión con Google".
 * Comprueba firma (claves públicas de Google), emisor, audiencia (nuestro
 * Client ID) y expiración. El id del usuario es el campo "sub" de Google.
 */
export class AdaptadorGoogle implements ProveedorDeAutenticacion {
  private readonly claves: JWTVerifyGetKey;

  /**
   * @param clientId ID de cliente de OAuth de Google, ej. "1050...apps.googleusercontent.com"
   * @param claves   solo para pruebas: permite inyectar claves locales en vez de las de Google
   */
  constructor(private readonly clientId: string, claves?: JWTVerifyGetKey) {
    this.claves = claves ?? createRemoteJWKSet(new URL(URL_CLAVES_GOOGLE));
  }

  async verificarToken(token: string): Promise<UsuarioAutenticado> {
    try {
      const { payload } = await jwtVerify(token, this.claves, {
        issuer: EMISORES_GOOGLE,
        audience: this.clientId,
        algorithms: ['RS256'],
      });
      if (!payload.sub) throw new ErrorNoAutenticado();
      return {
        id: payload.sub,
        correo: typeof payload.email === 'string' ? payload.email : undefined,
      };
    } catch (error) {
      if (error instanceof ErrorNoAutenticado) throw error;
      throw new ErrorNoAutenticado();
    }
  }
}