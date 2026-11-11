/**
 * Descripcion de cada tabla para la pantalla generica de catalogos.
 * - texto / largo / numero / fecha: se mandan tal cual.
 * - ref: es una relacion; se escribe el id y se manda como {"clave": {"id": N}}.
 */
export type TipoCampo = 'texto' | 'largo' | 'numero' | 'fecha' | 'ref';

export interface Campo {
  clave: string;
  etiqueta: string;
  tipo: TipoCampo;
  /** Solo para ref: nombre de la clave del id en el objeto relacionado (por defecto "id"). */
  refId?: string;
}

export interface Recurso {
  ruta: string;
  nombre: string;
  /** Nombre de la clave del id de la propia tabla (por defecto "id"). */
  idClave?: string;
  /** Muestra una caja de busqueda por nombre (?q=). */
  busqueda?: boolean;
  campos: Campo[];
}

const t = (clave: string, etiqueta: string): Campo => ({ clave, etiqueta, tipo: 'texto' });
const n = (clave: string, etiqueta: string): Campo => ({ clave, etiqueta, tipo: 'numero' });
const r = (clave: string, etiqueta: string, refId = 'id'): Campo => ({ clave, etiqueta, tipo: 'ref', refId });

export const RECURSOS: Recurso[] = [
  { ruta: 'titulos', nombre: 'Títulos', busqueda: true, campos: [
    n('tmdbId', 'TMDB id'), n('tipoId', 'Tipo (id)'), t('nombre', 'Nombre'),
    { clave: 'sinopsis', etiqueta: 'Sinopsis', tipo: 'largo' }, n('anio', 'Año'),
    n('calificacionTmdb', 'Calificación TMDB'), t('imagenPoster', 'Imagen póster'), t('imagenFondo', 'Imagen fondo') ] },
  { ruta: 'lista', nombre: 'Lista de usuario', campos: [
    n('usuarioId', 'Usuario (id)'), r('titulo', 'Título (id)'), n('estadoId', 'Estado (id)'),
    n('calificacion', 'Calificación (0-10)'), { clave: 'resena', etiqueta: 'Reseña', tipo: 'largo' } ] },
  { ruta: 'favoritos', nombre: 'Favoritos', campos: [ n('usuarioId', 'Usuario (id)'), r('titulo', 'Título (id)') ] },
  { ruta: 'reparto', nombre: 'Reparto', campos: [
    r('titulo', 'Título (id)'), r('actor', 'Actor (id)', 'idActor'), n('personaId', 'Persona (id)'),
    n('orden', 'Orden'), t('personaje', 'Personaje') ] },
  { ruta: 'actores', nombre: 'Actores', idClave: 'idActor', campos: [
    t('nombre', 'Nombre'), t('direccion', 'Dirección'), t('telefono', 'Teléfono'), t('genero', 'Género'), t('pais', 'País') ] },
  { ruta: 'titulo-genero', nombre: 'Título - Género', campos: [ r('titulo', 'Título (id)'), n('generoId', 'Género (id)') ] },
  { ruta: 'titulo-pais', nombre: 'Título - País', campos: [ r('titulo', 'Título (id)'), r('pais', 'País (id)') ] },
  { ruta: 'disponibilidad', nombre: 'Disponibilidad', campos: [
    r('tituloPais', 'Título-País (id)'), n('plataformaId', 'Plataforma (id)'), r('tipoDisponibilidad', 'Tipo disponibilidad (id)') ] },
  { ruta: 'paises', nombre: 'Países', campos: [ t('codigo', 'Código'), t('nombre', 'Nombre'), t('idioma', 'Idioma'), t('region', 'Región') ] },
  { ruta: 'tipos-disponibilidad', nombre: 'Tipos de disponibilidad', campos: [ t('clave', 'Clave'), t('etiqueta', 'Etiqueta') ] },
  { ruta: 'generos', nombre: 'Géneros', campos: [ n('tmdbId', 'TMDB id'), t('nombre', 'Nombre') ] },
  { ruta: 'personas', nombre: 'Personas', campos: [
    n('tmdbId', 'TMDB id'), t('nombre', 'Nombre'), t('sexo', 'Sexo'),
    { clave: 'fechaNacimiento', etiqueta: 'Fecha de nacimiento', tipo: 'fecha' }, t('nacionalidad', 'Nacionalidad') ] },
  { ruta: 'plataformas', nombre: 'Plataformas', campos: [
    n('tmdbId', 'TMDB id'), t('nombre', 'Nombre'), t('pais', 'País'), n('precioMensual', 'Precio mensual') ] },
  { ruta: 'tipos-titulo', nombre: 'Tipos de título', campos: [ t('clave', 'Clave'), t('etiqueta', 'Etiqueta') ] },
  { ruta: 'estados-lista', nombre: 'Estados de lista', campos: [ t('clave', 'Clave'), t('etiqueta', 'Etiqueta') ] },
];
