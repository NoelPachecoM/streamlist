import type { VercelRequest } from '@vercel/node';
import { usuarioDeLaPeticion } from './_autenticar';
import { repositorioDeBitacora, repositorioDeUsuarios } from './_composicion';
import type { EntradaBitacora } from './adaptadores/supabase/AdaptadorSupabaseBitacora';
import type { UsuarioAplicacion } from './adaptadores/supabase/AdaptadorSupabaseUsuarios';
import { ErrorNoAutorizado } from './dominio/errores';

/** Autentica la petición y exige que la cuenta tenga rol 1 (administrador). */
export async function exigirAdministrador(req: VercelRequest): Promise<UsuarioAplicacion> {
  const identidad = await usuarioDeLaPeticion(req);
  const administrador = await repositorioDeUsuarios().obtenerPorId(identidad.id);
  if (!administrador || administrador.rolId !== 1) throw new ErrorNoAutorizado();
  return administrador;
}

/** Registra una acción en la bitácora. Si falla, no deshace la acción ya realizada. */
export async function anotarEnBitacora(entrada: EntradaBitacora): Promise<void> {
  try {
    await repositorioDeBitacora().registrar(entrada);
  } catch (error) {
    console.error('No se pudo escribir en la bitácora', error);
  }
}
