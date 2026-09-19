import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { Country } from '../../domain/models/country.model';

@Injectable({ providedIn: 'root' })
export class ListCountriesUseCase {
  private readonly authRepository = inject(AuthRepository)

  execute(): Observable<Country[]> {
    return this.authRepository.getCountries();
  }
}
