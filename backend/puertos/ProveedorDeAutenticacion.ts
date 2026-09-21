export interface UsuarioAutenticado {
  id: string;
  correo?: string;
}

/**
 * Puerto: cómo sabemos quién hace la petición.
 * La verificación en dos pasos (QR) la resuelve el proveedor (Auth0) antes de
 * emitir el token, así que el núcleo solo necesita validar el token.
 */
export interface ProveedorDeAutenticacion {
  /** Lanza ErrorNoAutenticado si el token no es válido. */
  verificarToken(token: string): Promise<UsuarioAutenticado>;
}
