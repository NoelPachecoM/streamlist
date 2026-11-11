// Copia la página de Angular ya compilada (dist/streamlist/browser) a la carpeta static de Spring,
// para que Spring la sirva en http://<tu-ip> o http://noel.local. Se usa con: npm run build:spring
import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';

const origen = 'dist/streamlist/browser';
const destino = 'spring-practica/src/main/resources/static';

if (!existsSync(origen)) {
  console.error(`No existe ${origen}. Revisa que "ng build" haya terminado sin errores.`);
  process.exit(1);
}
rmSync(destino, { recursive: true, force: true });
mkdirSync(destino, { recursive: true });
cpSync(origen, destino, { recursive: true });
console.log(`Listo: la página quedó en ${destino}. Reinicia Spring para verla.`);
