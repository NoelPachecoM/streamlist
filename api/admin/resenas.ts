import type { VercelRequest, VercelResponse } from '@vercel/node';
import { anotarEnBitacora, exigirAdministrador } from '../../backend/_administracion';
import { repositorioDeResenas } from '../../backend/_composicion';
import { prepararRespuesta, primerValor, responderError } from '../../backend/_http';
import { esUuid } from '../../backend/adaptadores/supabase/ayudasPerfiles';
import { ErrorDeValidacion, ErrorNoEncontrado } from '../../backend/dominio/errores';

const METODOS = ['GET', 'DELETE'];

export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);
  if (!METODOS.includes(req.method ?? '')) {
    res.setHeader('Allow', METODOS.join(', '));
    res.status(405).json({ error: { codigo: 'METODO_NO_PERMITIDO', mensaje: `Usa ${METODOS.join(' o ')}` } });
    return;
  }

  try {
    const administrador = await exigirAdministrador(req);
    const repositorio = repositorioDeResenas();

    if (req.method === 'GET') {
      res.status(200).json(await repositorio.listar());
      return;
    }

    const id = primerValor(req.query['id']);
    if (!id || !esUuid(id)) throw new ErrorDeValidacion('Falta el id de la reseña o no es válido');

    const eliminada = await repositorio.quitarTexto(id);
    if (!eliminada) throw new ErrorNoEncontrado('Reseña no encontrada');

    await anotarEnBitacora({
      adminId: administrador.id,
      accion: 'resena_eliminada',
      entidad: 'lista_usuario',
      entidadId: eliminada.id,
      detalle: {
        usuarioId: eliminada.usuarioId,
        usuario: eliminada.usuarioNombre,
        tmdbId: eliminada.tmdbId,
        tipo: eliminada.tipo,
        titulo: eliminada.titulo,
        calificacion: eliminada.calificacion,
        textoEliminado: eliminada.resena,
      },
    });

    res.status(200).json({ id });
  } catch (error) {
    responderError(res, error);
  }
}
