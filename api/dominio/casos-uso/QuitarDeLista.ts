import type { RepositorioDeLista } from '../../puertos/RepositorioDeLista';
import type { UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';
import { ErrorDeValidacion } from '../errores';

const REGEX_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export class QuitarDeLista {
  constructor(private readonly lista: RepositorioDeLista) {}

  async ejecutar(usuario: UsuarioAutenticado, idItem: string): Promise<void> {
    if (!REGEX_UUID.test(idItem)) throw new ErrorDeValidacion('ID de elemento inválido');
    await this.lista.eliminar(usuario.id, idItem);
  }
}
