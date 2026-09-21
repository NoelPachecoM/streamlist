import { ESTADOS_LISTA, TIPOS_TITULO } from '../entidades/ItemLista';
import type { EstadoLista, ItemLista } from '../entidades/ItemLista';
import type { TipoTitulo } from '../entidades/Titulo';
import type { RepositorioDeLista } from '../../puertos/RepositorioDeLista';
import type { UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';
import { ErrorDeValidacion } from '../errores';

export interface DatosNuevoItem {
  tmdbId: number;
  tipo: string;
  estado?: string;
}

export class AgregarALista {
  constructor(private readonly lista: RepositorioDeLista) {}

  async ejecutar(usuario: UsuarioAutenticado, datos: DatosNuevoItem): Promise<ItemLista> {
    if (!Number.isInteger(datos.tmdbId) || datos.tmdbId <= 0) throw new ErrorDeValidacion('ID de título inválido');
    if (!TIPOS_TITULO.includes(datos.tipo as TipoTitulo)) throw new ErrorDeValidacion('Tipo inválido (pelicula | serie)');
    const estado = datos.estado ?? 'por_ver';
    if (!ESTADOS_LISTA.includes(estado as EstadoLista)) throw new ErrorDeValidacion('Estado inválido');

    return this.lista.agregar({
      usuarioId: usuario.id,
      tmdbId: datos.tmdbId,
      tipo: datos.tipo as TipoTitulo,
      estado: estado as EstadoLista,
    });
  }
}
