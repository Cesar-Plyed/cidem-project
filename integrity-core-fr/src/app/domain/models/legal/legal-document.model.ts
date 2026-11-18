/** Documentos legales públicos (aviso legal, privacidad, cookies y términos). */

export type LegalDocumentId =
  | 'aviso-legal'
  | 'privacidad'
  | 'cookies'
  | 'terminos';

export interface LegalSection {
  heading: string;
  paragraphs?: string[];
  items?: string[];
}

export interface LegalDocument {
  id: LegalDocumentId;
  title: string;
  updatedAt: string;
  intro?: string;
  sections: LegalSection[];
}
