/** Errores del dominio. Los handlers HTTP los traducen a códigos de estado. */
export class ErrorDeDominio extends Error {
  constructor(mensaje: string, public readonly codigo: string) {
    super(mensaje);
  }
}

export class ErrorDeValidacion extends ErrorDeDominio {
  constructor(mensaje: string) { super(mensaje, 'VALIDACION'); }
}
export class ErrorNoAutenticado extends ErrorDeDominio {
  constructor(mensaje = 'Sesión inválida o expirada') { super(mensaje, 'NO_AUTENTICADO'); }
}
export class ErrorNoEncontrado extends ErrorDeDominio {
  constructor(mensaje = 'Recurso no encontrado') { super(mensaje, 'NO_ENCONTRADO'); }
}
export class ErrorItemDuplicado extends ErrorDeDominio {
  constructor(mensaje = 'Ese título ya está en tu lista') { super(mensaje, 'DUPLICADO'); }
}
export class ErrorProveedorExterno extends ErrorDeDominio {
  constructor(mensaje = 'El servicio externo no respondió correctamente') { super(mensaje, 'PROVEEDOR_EXTERNO'); }
}
export class ErrorDeAlmacenamiento extends ErrorDeDominio {
  constructor(mensaje = 'No se pudo acceder al almacenamiento') { super(mensaje, 'ALMACENAMIENTO'); }
}
