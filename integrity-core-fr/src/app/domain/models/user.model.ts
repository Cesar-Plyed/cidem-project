import { Role } from './role.enum';

export interface User {
  id: string;
  firstNames: string;
  lastNames: string;
  email: string;
  countryName: string;
  role: Role;
}
