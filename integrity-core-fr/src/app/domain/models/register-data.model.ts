import { Role } from './role.enum';

export interface RegisterData {
  firstNames: string;
  lastNames: string;
  email: string;
  password: string;
  countryId: string; // UUID
  phoneNumber?: string;
  role: Role;
  registrationCode?: string; // obligatorio solo si role es PROFESOR o ADMINISTRATIVO
}
