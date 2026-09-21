import type { VercelRequest, VercelResponse } from '@vercel/node';
import { construirBuscarTitulos } from './_composicion';
import { prepararRespuesta, primerValor, responderError, soloMetodo } from './_http';

// GET /api/buscar?q=batman&pagina=1
export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);
  if (!soloMetodo(req, res, 'GET')) return;
  try {
    const consulta = primerValor(req.query['q']) ?? '';
    const pagina = Number(primerValor(req.query['pagina']) ?? 1);
    const resultado = await construirBuscarTitulos().ejecutar(consulta, pagina);
    res.status(200).json(resultado);
  } catch (error) {
    responderError(res, error);
  }
}
