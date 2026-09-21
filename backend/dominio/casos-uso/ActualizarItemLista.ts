import { ESTADOS_LISTA } from '../entidades/ItemLista';
import type { EstadoLista, ItemLista } from '../entidades/ItemLista';
import type { CambiosItemLista, RepositorioDeLista } from '../../puertos/RepositorioDeLista';
import type { UsuarioAutenticado } from '../../puertos/ProveedorDeAutenticacion';
import { ErrorDeValidacion } from '../errores';

const REGEX_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface DatosActualizacion {
  estado?: string;
  calificacion?: number | null;
  resena?: string | null;
}

export class ActualizarItemLista {
  constructor(private readonly lista: RepositorioDeLista) {}

  async ejecutar(usuario: UsuarioAutenticado, idItem: string, datos: DatosActualizacion): Promise<ItemLista> {
    if (!REGEX_UUID.test(idItem)) throw new ErrorDeValidacion('ID de elemento inválido');

    const cambios: CambiosItemLista = {};

    if (datos.estado !== undefined) {
      if (!ESTADOS_LISTA.includes(datos.estado as EstadoLista)) throw new ErrorDeValidacion('Estado inválido');
      cambios.estado = datos.estado as EstadoLista;
    }
    if (datos.calificacion !== undefined) {
      const c = datos.calificacion;
      if (c !== null && (!Number.isInteger(c) || c < 1 || c > 10)) {
        throw new ErrorDeValidacion('La calificación debe ser un entero de 1 a 10');
      }
      cambios.calificacion = c;
    }
    if (datos.resena !== undefined) {
      const r = datos.resena === null ? null : datos.resena.trim();
      if (r !== null && r.length > 1000) throw new ErrorDeValidacion('La reseña no puede pasar de 1000 caracteres');
      cambios.resena = r === '' ? null : r;
    }
    if (Object.keys(cambios).length === 0) throw new ErrorDeValidacion('No hay cambios que aplicar');

    return this.lista.actualizar(usuario.id, idItem, cambios);
  }
}
