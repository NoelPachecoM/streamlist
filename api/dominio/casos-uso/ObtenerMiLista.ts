import { ESTADOS_LISTA } from '../entidades/ItemLista';
import type { EstadoLista, ItemLista } from '../entidades/ItemLista';
import type { RepositorioDeLista } from '../../puertos/RepositorioDeLista';
import type { UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';
import { ErrorDeValidacion } from '../errores';

export class ObtenerMiLista {
  constructor(private readonly lista: RepositorioDeLista) {}

  async ejecutar(usuario: UsuarioAutenticado, estado?: string): Promise<ItemLista[]> {
    if (estado !== undefined && !ESTADOS_LISTA.includes(estado as EstadoLista)) {
      throw new ErrorDeValidacion('Estado inválido');
    }
    return this.lista.obtenerPorUsuario(usuario.id, estado as EstadoLista | undefined);
  }
}
