import { Observable } from 'rxjs';
import { AuthCredentials } from '../models/auth-credentials.model';
import { RegisterData } from '../models/register-data.model';
import { AuthTokens } from '../models/auth-tokens.model';
import { User } from '../models/user.model';
import { Country } from '../models/country.model';
import { DirectoryUser } from '../models/user-directory.model';

/**
 * Puerto de salida (hexagonal): define QUÉ se puede hacer para autenticar,
 * sin saber CÓMO se hace (HTTP, mock, etc.). La implementación real vive
 * en infrastructure/http/auth-http.repository.ts.
 */
export abstract class AuthRepository {
  abstract getCountries(): Observable<Country[]>;
  abstract getUsers(): Observable<DirectoryUser[]>;
  abstract login(credentials: AuthCredentials): Observable<AuthTokens>;
  abstract register(data: RegisterData): Observable<User>;
  abstract refresh(refreshToken: string): Observable<AuthTokens>;
  abstract logout(refreshToken: string): Observable<void>;
}
