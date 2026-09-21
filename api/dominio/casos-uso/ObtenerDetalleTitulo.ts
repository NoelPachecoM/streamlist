import { TIPOS_TITULO } from '../entidades/ItemLista';
import type { DetalleTitulo, TipoTitulo } from '../entidades/Titulo';
import type { ProveedorDePeliculas } from '../../puertos/ProveedorDePeliculas';
import { ErrorDeValidacion } from '../errores';

export class ObtenerDetalleTitulo {
  constructor(private readonly peliculas: ProveedorDePeliculas) {}

  async ejecutar(tmdbId: number, tipo: string, pais = 'MX'): Promise<DetalleTitulo> {
    if (!Number.isInteger(tmdbId) || tmdbId <= 0) throw new ErrorDeValidacion('ID de título inválido');
    if (!TIPOS_TITULO.includes(tipo as TipoTitulo)) throw new ErrorDeValidacion('Tipo inválido (pelicula | serie)');
    const codigoPais = pais.toUpperCase();
    if (!/^[A-Z]{2}$/.test(codigoPais)) throw new ErrorDeValidacion('País inválido (código de 2 letras)');
    return this.peliculas.obtenerDetalle(tmdbId, tipo as TipoTitulo, codigoPais);
  }
}
