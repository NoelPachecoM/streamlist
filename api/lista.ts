import type { VercelRequest, VercelResponse } from '@vercel/node';
import { usuarioDeLaPeticion } from './_autenticar';
import {
  construirActualizarItemLista,
  construirAgregarALista,
  construirObtenerMiLista,
  construirQuitarDeLista,
} from './_composicion';
import { prepararRespuesta, primerValor, responderError } from './_http';
import type { DatosActualizacion } from './dominio/casos-uso/ActualizarItemLista';
import { ErrorDeValidacion } from './dominio/errores';

/**
 * Lista personal del usuario (requiere "Authorization: Bearer <token de Auth0>"):
 *   GET    /api/lista?estado=por_ver     -> lista (el filtro es opcional)
 *   POST   /api/lista                    -> body: { tmdbId, tipo, estado? }
 *   PATCH  /api/lista?id=<uuid>          -> body: { estado?, calificacion?, resena? }
 *   DELETE /api/lista?id=<uuid>
 */
const METODOS = ['GET', 'POST', 'PATCH', 'DELETE'];

export default async function handler(req: VercelRequest, res: VercelResponse): Promise<void> {
  prepararRespuesta(res);

  if (!METODOS.includes(req.method ?? '')) {
    res.setHeader('Allow', METODOS.join(', '));
    res.status(405).json({ error: { codigo: 'METODO_NO_PERMITIDO', mensaje: `Usa ${METODOS.join(', ')}` } });
    return;
  }

  try {
    const usuario = await usuarioDeLaPeticion(req);

    if (req.method === 'GET') {
      const items = await construirObtenerMiLista().ejecutar(usuario, primerValor(req.query['estado']));
      res.status(200).json(items);
      return;
    }

    if (req.method === 'POST') {
      const cuerpo = comoObjeto(req.body);
      const item = await construirAgregarALista().ejecutar(usuario, {
        tmdbId: Number(cuerpo['tmdbId']),
        tipo: texto(cuerpo['tipo']) ?? '',
        estado: texto(cuerpo['estado']),
      });
      res.status(201).json(item);
      return;
    }

    const id = primerValor(req.query['id']) ?? '';

    if (req.method === 'PATCH') {
      const cuerpo = comoObjeto(req.body);
      const datos: DatosActualizacion = {};
      if ('estado' in cuerpo) datos.estado = texto(cuerpo['estado']);
      if ('calificacion' in cuerpo) datos.calificacion = numeroONulo(cuerpo['calificacion'], 'calificacion');
      if ('resena' in cuerpo) datos.resena = textoONulo(cuerpo['resena'], 'resena');
      const item = await construirActualizarItemLista().ejecutar(usuario, id, datos);
      res.status(200).json(item);
      return;
    }

    // DELETE
    await construirQuitarDeLista().ejecutar(usuario, id);
    res.status(204).end();
  } catch (error) {
    responderError(res, error);
  }
}

// ---- utilidades para leer el cuerpo de la petición ----

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

function texto(valor: unknown): string | undefined {
  if (valor === undefined) return undefined;
  if (typeof valor !== 'string') throw new ErrorDeValidacion('Se esperaba un texto');
  return valor;
}

function textoONulo(valor: unknown, campo: string): string | null {
  if (valor === null) return null;
  if (typeof valor !== 'string') throw new ErrorDeValidacion(`"${campo}" debe ser texto o null`);
  return valor;
}

function numeroONulo(valor: unknown, campo: string): number | null {
  if (valor === null) return null;
  if (typeof valor !== 'number') throw new ErrorDeValidacion(`"${campo}" debe ser un número o null`);
  return valor;
}
