import type { ResultadoBusqueda } from '../entidades/Titulo';
import type { ProveedorDePeliculas } from '../../puertos/ProveedorDePeliculas';
import { ErrorDeValidacion } from '../errores';

export class BuscarTitulos {
  constructor(private readonly peliculas: ProveedorDePeliculas) {}

  async ejecutar(consulta: string, pagina = 1): Promise<ResultadoBusqueda> {
    const texto = (consulta ?? '').trim();
    if (texto.length < 2) throw new ErrorDeValidacion('La búsqueda debe tener al menos 2 caracteres');
    if (texto.length > 100) throw new ErrorDeValidacion('La búsqueda es demasiado larga');
    if (!Number.isInteger(pagina) || pagina < 1 || pagina > 500) {
      throw new ErrorDeValidacion('Página inválida');
    }
    return this.peliculas.buscar(texto, pagina);
  }
}
