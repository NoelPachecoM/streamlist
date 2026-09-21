import type { DetalleTitulo, ResultadoBusqueda, TipoTitulo } from '../../dominio/entidades/Titulo';
import type { CacheDeDetalles } from '../../puertos/CacheDeDetalles';
import type { ProveedorDePeliculas } from '../../puertos/ProveedorDePeliculas';

/**
 * Decorador: implementa el mismo puerto ProveedorDePeliculas, pero antes de
 * pedirle el detalle a TMDB revisa el caché. Si el caché falla, se ignora el
 * error y se sigue con TMDB: el caché nunca debe romper la aplicación.
 */
export class ProveedorDePeliculasConCache implements ProveedorDePeliculas {
  constructor(
    private readonly proveedor: ProveedorDePeliculas,
    private readonly cache: CacheDeDetalles,
  ) {}

  buscar(consulta: string, pagina: number): Promise<ResultadoBusqueda> {
    return this.proveedor.buscar(consulta, pagina);
  }

  async obtenerDetalle(tmdbId: number, tipo: TipoTitulo, pais: string): Promise<DetalleTitulo> {
    const clavePais = pais.toUpperCase();

    try {
      const guardado = await this.cache.obtener(tmdbId, tipo, clavePais);
      if (guardado) return guardado;
    } catch (error) {
      console.error('[cache] no se pudo leer:', error);
    }

    const detalle = await this.proveedor.obtenerDetalle(tmdbId, tipo, pais);

    try {
      await this.cache.guardar(detalle, clavePais);
    } catch (error) {
      console.error('[cache] no se pudo guardar:', error);
    }

    return detalle;
  }
}