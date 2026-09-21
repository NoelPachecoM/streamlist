import type { VercelRequest } from '@vercel/node';
import { proveedorDeAutenticacion } from './_composicion';
import { ErrorNoAutenticado } from './dominio/errores';
import type { UsuarioAutenticado } from './puertos/ProveedorDeAutenticacion';

/** Lee el header "Authorization: Bearer <token>" y devuelve al usuario verificado. */
export async function usuarioDeLaPeticion(req: VercelRequest): Promise<UsuarioAutenticado> {
  const cabecera = req.headers['authorization'];
  const valor = Array.isArray(cabecera) ? cabecera[0] : cabecera;
  const coincidencia = /^Bearer\s+(\S+)$/i.exec(valor ?? '');
  const token = coincidencia?.[1];
  if (!token) throw new ErrorNoAutenticado('Falta el token de sesión');
  return proveedorDeAutenticacion().verificarToken(token);
}
