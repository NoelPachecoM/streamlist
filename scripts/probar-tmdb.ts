// Uso: npx tsx --env-file=.env scripts/probar-tmdb.ts
import { AdaptadorTMDB } from '../api/adaptadores/tmdb/AdaptadorTMDB';
import { BuscarTitulos } from '../api/dominio/casos-uso/BuscarTitulos';
import { ObtenerDetalleTitulo } from '../api/dominio/casos-uso/ObtenerDetalleTitulo';

const token = process.env['TMDB_READ_TOKEN'];
if (!token) throw new Error('Falta TMDB_READ_TOKEN en el .env');

const proveedor = new AdaptadorTMDB(token);

const busqueda = await new BuscarTitulos(proveedor).ejecutar('batman');
console.log(`\nBúsqueda "batman": ${busqueda.totalResultados} resultados. Primeros 3:`);
for (const t of busqueda.resultados.slice(0, 3)) console.log(` - [${t.tipo}] ${t.nombre} (${t.anio}) id=${t.tmdbId}`);

const detalle = await new ObtenerDetalleTitulo(proveedor).ejecutar(550, 'pelicula', 'MX');
console.log(`\nDetalle: ${detalle.nombre} (${detalle.anio}) — ${detalle.generos.join(', ')}`);
console.log('Reparto:', detalle.reparto.slice(0, 3).map((r) => r.nombre).join(', '));
console.log('Dónde verla en MX (suscripción):', detalle.disponibilidad.suscripcion.map((p) => p.nombre).join(', ') || 'ninguna');
