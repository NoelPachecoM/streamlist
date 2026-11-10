import type { VercelRequest, VercelResponse } from '@vercel/node';
import { usuarioDeLaPeticion } from '../backend/_autenticar';
import { repositorioDeUsuarios } from '../backend/_composicion';
import { prepararRespuesta, responderError, soloMetodo } from '../backend/_http';

export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);
  if (!soloMetodo(req, res, 'GET')) return;

  try {
    const identidad = await usuarioDeLaPeticion(req);
    const perfil = await repositorioDeUsuarios().guardarPerfil(identidad);
    res.status(200).json(perfil);
  } catch (error) {
    responderError(res, error);
  }
}