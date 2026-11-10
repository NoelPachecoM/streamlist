import type { VercelRequest, VercelResponse } from '@vercel/node';
import { exigirAdministrador } from '../../backend/_administracion';
import { repositorioDeBitacora } from '../../backend/_composicion';
import { prepararRespuesta, responderError } from '../../backend/_http';

export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    res.status(405).json({ error: { codigo: 'METODO_NO_PERMITIDO', mensaje: 'Usa GET' } });
    return;
  }

  try {
    await exigirAdministrador(req);
    res.status(200).json(await repositorioDeBitacora().listar());
  } catch (error) {
    responderError(res, error);
  }
}
