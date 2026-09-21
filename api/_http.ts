import type { VercelRequest, VercelResponse } from '@vercel/node';
import { ErrorDeDominio } from './dominio/errores';

const ESTADO_HTTP: Record<string, number> = {
  VALIDACION: 400,
  NO_AUTENTICADO: 401,
  NO_ENCONTRADO: 404,
  DUPLICADO: 409,
  PROVEEDOR_EXTERNO: 502,
  ALMACENAMIENTO: 500,
};

/** Evita respuestas 304 viejas durante las pruebas. */
export function prepararRespuesta(res: VercelResponse): void {
  res.setHeader('Cache-Control', 'no-store');
}

export function soloMetodo(req: VercelRequest, res: VercelResponse, metodo: string): boolean {
  if (req.method === metodo) return true;
  res.setHeader('Allow', metodo);
  res.status(405).json({ error: { codigo: 'METODO_NO_PERMITIDO', mensaje: `Usa ${metodo}` } });
  return false;
}

export function primerValor(valor: string | string[] | undefined): string | undefined {
  return Array.isArray(valor) ? valor[0] : valor;
}

export function responderError(res: VercelResponse, error: unknown): void {
  if (error instanceof ErrorDeDominio) {
    res.status(ESTADO_HTTP[error.codigo] ?? 400).json({ error: { codigo: error.codigo, mensaje: error.message } });
    return;
  }
  console.error(error);
  res.status(500).json({ error: { codigo: 'INTERNO', mensaje: 'Error interno del servidor' } });
}
