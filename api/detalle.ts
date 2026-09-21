import type { VercelRequest, VercelResponse } from '@vercel/node';
import { construirObtenerDetalleTitulo } from './_composicion';
import { prepararRespuesta, primerValor, responderError, soloMetodo } from './_http';

// GET /api/detalle?id=550&tipo=pelicula&pais=MX
export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);
  if (!soloMetodo(req, res, 'GET')) return;
  try {
    const id = Number(primerValor(req.query['id']));
    const tipo = primerValor(req.query['tipo']) ?? '';
    const pais = primerValor(req.query['pais']) ?? 'MX';
    const detalle = await construirObtenerDetalleTitulo().ejecutar(id, tipo, pais);
    res.status(200).json(detalle);
  } catch (error) {
    responderError(res, error);
  }
}
