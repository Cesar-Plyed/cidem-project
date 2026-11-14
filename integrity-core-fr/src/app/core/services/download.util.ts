import { ExportArtifact } from '../../domain/models/legal/legal.model';

/**
 * Descarga un Blob en el navegador y devuelve sus metadatos. En jsdom (tests)
 * URL.createObjectURL no existe: se usa una URL simulada para poder verificar
 * que el enlace se genero.
 */
export function saveBlob(blob: Blob, filename: string, format: string): ExportArtifact {
  const canCreateUrl = typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function';
  const url = canCreateUrl ? URL.createObjectURL(blob) : `blob:mock/${filename}`;

  // En jsdom no hay navegacion real: se omite el click para no ensuciar la salida.
  if (canCreateUrl && typeof document !== 'undefined') {
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
  }

  return { url, size: blob.size, generatedAt: new Date().toISOString(), format };
}
