import type { VercelRequest, VercelResponse } from '@vercel/node';
import { anotarEnBitacora, exigirAdministrador } from '../../backend/_administracion';
import { repositorioDeUsuarios } from '../../backend/_composicion';
import { prepararRespuesta, primerValor, responderError } from '../../backend/_http';
import { ErrorNoEncontrado, ErrorDeValidacion } from '../../backend/dominio/errores';
import type { RolId } from '../../backend/adaptadores/supabase/AdaptadorSupabaseUsuarios';

const METODOS = ['GET', 'PATCH'];

export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);
  if (!METODOS.includes(req.method ?? '')) {
    res.setHeader('Allow', METODOS.join(', '));
    res.status(405).json({ error: { codigo: 'METODO_NO_PERMITIDO', mensaje: `Usa ${METODOS.join(' o ')}` } });
    return;
  }

  try {
    const administrador = await exigirAdministrador(req);
    const repositorio = repositorioDeUsuarios();

    if (req.method === 'GET') {
      res.status(200).json(await repositorio.obtenerTodos());
      return;
    }

    const id = primerValor(req.query['id']);
    if (!id) throw new ErrorDeValidacion('Falta el id del usuario');
    if (id === administrador.id) throw new ErrorDeValidacion('No puedes cambiar tu propio rol');

    const cuerpo = comoObjeto(req.body);
    const rolId = cuerpo['rolId'];
    if (rolId !== 1 && rolId !== 2) throw new ErrorDeValidacion('rolId debe ser 1 (administrador) o 2 (usuario)');

    const anterior = await repositorio.obtenerPorId(id);
    if (!anterior) throw new ErrorNoEncontrado('Usuario no encontrado');

    const usuario = await repositorio.cambiarRol(id, rolId as RolId);
    if (!usuario) throw new ErrorNoEncontrado('Usuario no encontrado');

    if (anterior.rolId !== usuario.rolId) {
      await anotarEnBitacora({
        adminId: administrador.id,
        accion: 'rol_cambiado',
        entidad: 'perfiles',
        entidadId: usuario.id,
        detalle: {
          nombre: usuario.nombre,
          correo: usuario.correo,
          rolAnterior: anterior.rolId,
          rolNuevo: usuario.rolId,
        },
      });
    }

    res.status(200).json(usuario);
  } catch (error) {
    responderError(res, error);
  }
}

function comoObjeto(valor: unknown): Record<string, unknown> {
  let dato = valor;
  if (typeof dato === 'string') {
    try {
      dato = JSON.parse(dato);
    } catch {
      throw new ErrorDeValidacion('El cuerpo no es un JSON válido');
    }
  }
  if (typeof dato !== 'object' || dato === null || Array.isArray(dato)) {
    throw new ErrorDeValidacion('El cuerpo debe ser un objeto JSON');
  }
  return dato as Record<string, unknown>;
}
