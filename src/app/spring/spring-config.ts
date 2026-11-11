/**
 * Dirección de la API de Spring, según dónde se abra la página:
 *
 * - paginanoel.online (Vercel) o *.vercel.app  -> https://api.paginanoel.online (Spring en Render)
 * - localhost:4200 (npm start)                 -> http://noel.local (Spring en tu computadora)
 * - Página servida por el mismo Spring         -> la misma dirección (noel.local, tu IP, etc.)
 */
const API_DOMINIO = 'https://api.paginanoel.online';
const API_DESARROLLO = 'http://noel.local';

function calcularSpringUrl(): string {
  if (typeof window === 'undefined') return API_DOMINIO;
  const { protocol, host, hostname, port } = window.location;
  if (hostname === 'paginanoel.online' || hostname.endsWith('.paginanoel.online') || hostname.endsWith('.vercel.app')) {
    return API_DOMINIO;
  }
  if (port === '4200') return API_DESARROLLO;
  return `${protocol}//${host}`;
}

export const SPRING_URL = calcularSpringUrl();
