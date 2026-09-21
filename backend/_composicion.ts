/**
 * Raíz de composición: el ÚNICO lugar que sabe qué adaptador concreto se usa
 * para cada puerto. Para cambiar de TMDB o de Supabase, solo se toca este archivo.
 */
import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';
import { ProveedorDePeliculasConCache } from './adaptadores/cache/ProveedorDePeliculasConCache';
import { AdaptadorGoogle } from './adaptadores/google/AdaptadorGoogle';
import { AdaptadorSupabaseCache } from './adaptadores/supabase/AdaptadorSupabaseCache';
import { AdaptadorSupabaseLista } from './adaptadores/supabase/AdaptadorSupabaseLista';
import { AdaptadorTMDB } from './adaptadores/tmdb/AdaptadorTMDB';
import { ActualizarItemLista } from './dominio/casos-uso/ActualizarItemLista';
import { AgregarALista } from './dominio/casos-uso/AgregarALista';
import { BuscarTitulos } from './dominio/casos-uso/BuscarTitulos';
import { ObtenerDetalleTitulo } from './dominio/casos-uso/ObtenerDetalleTitulo';
import { ObtenerMiLista } from './dominio/casos-uso/ObtenerMiLista';
import { QuitarDeLista } from './dominio/casos-uso/QuitarDeLista';
import type { ProveedorDeAutenticacion } from './puertos/ProveedorDeAutenticacion';
import type { ProveedorDePeliculas } from './puertos/ProveedorDePeliculas';
import type { RepositorioDeLista } from './puertos/RepositorioDeLista';

/** Un detalle guardado en el caché se considera fresco durante 24 horas. */
const VIGENCIA_CACHE_MS = 24 * 60 * 60 * 1000;

function variable(nombre: string): string {
  const valor = process.env[nombre];
  if (!valor) throw new Error(`Falta la variable de entorno ${nombre}`);
  return valor;
}

let supabase: SupabaseClient | undefined;
let peliculas: ProveedorDePeliculas | undefined;
let lista: RepositorioDeLista | undefined;
let autenticacion: ProveedorDeAutenticacion | undefined;

function clienteSupabase(): SupabaseClient {
  return (supabase ??= createClient(variable('SUPABASE_URL'), variable('SUPABASE_SERVICE_ROLE_KEY'), {
    auth: { persistSession: false, autoRefreshToken: false },
  }));
}

function proveedorDePeliculas(): ProveedorDePeliculas {
  return (peliculas ??= new ProveedorDePeliculasConCache(
    new AdaptadorTMDB(variable('TMDB_READ_TOKEN')),
    new AdaptadorSupabaseCache(clienteSupabase(), VIGENCIA_CACHE_MS),
  ));
}

export function proveedorDeAutenticacion(): ProveedorDeAutenticacion {
  return (autenticacion ??= new AdaptadorGoogle(variable('GOOGLE_CLIENT_ID')));
}

function repositorioDeLista(): RepositorioDeLista {
  return (lista ??= new AdaptadorSupabaseLista(clienteSupabase()));
}

export const construirBuscarTitulos = () => new BuscarTitulos(proveedorDePeliculas());
export const construirObtenerDetalleTitulo = () => new ObtenerDetalleTitulo(proveedorDePeliculas());
export const construirAgregarALista = () => new AgregarALista(repositorioDeLista());
export const construirObtenerMiLista = () => new ObtenerMiLista(repositorioDeLista());
export const construirActualizarItemLista = () => new ActualizarItemLista(repositorioDeLista());
export const construirQuitarDeLista = () => new QuitarDeLista(repositorioDeLista());