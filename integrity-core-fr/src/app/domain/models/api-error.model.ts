export interface ApiError {
  timestamp: string; // ISO-8601
  status: number;
  error: string;
  message: string;
  fieldErrors: Record<string, string> | null; // solo presente en errores 400 de validación
}
