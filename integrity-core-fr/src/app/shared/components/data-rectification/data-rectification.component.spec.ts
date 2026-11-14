import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DataRectificationComponent } from './data-rectification.component';
import { LegalApiService } from '../../../core/services/legal-api.service';

function buttonByText(root: HTMLElement, text: string): HTMLButtonElement {
  const button = Array.from(root.querySelectorAll('button'))
    .find((candidate) => candidate.textContent?.includes(text));
  if (!button) {
    throw new Error(`Boton no encontrado: ${text}`);
  }
  return button as HTMLButtonElement;
}

describe('DataRectificationComponent', () => {
  let rectifyData: ReturnType<typeof vi.fn>;
  let softDeleteData: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    rectifyData = vi.fn(() => of({}));
    softDeleteData = vi.fn(() => of({}));

    TestBed.configureTestingModule({
      imports: [DataRectificationComponent],
      providers: [{ provide: LegalApiService, useValue: { rectifyData, softDeleteData } }],
    });
  });

  it('los campos empiezan deshabilitados y Editar los habilita', () => {
    const fixture = TestBed.createComponent(DataRectificationComponent);
    fixture.componentRef.setInput('entityId', 'student-1');
    fixture.detectChanges();

    const email = fixture.nativeElement.querySelector('#rect-email') as HTMLInputElement;
    expect(email.disabled).toBe(true);

    buttonByText(fixture.nativeElement, 'Editar').click();
    fixture.detectChanges();

    expect(email.disabled).toBe(false);
    expect(fixture.componentInstance.editing()).toBe(true);
  });

  it('Guardar envia el PUT de rectificacion', () => {
    const fixture = TestBed.createComponent(DataRectificationComponent);
    fixture.componentRef.setInput('entityId', 'student-1');
    fixture.detectChanges();

    fixture.componentInstance.startEdit();
    fixture.componentInstance.fields.set({ email: 'nuevo@cidemags.edu', firstNames: 'Ana', lastNames: 'Lopez' });
    fixture.detectChanges();
    buttonByText(fixture.nativeElement, 'Guardar').click();

    expect(rectifyData).toHaveBeenCalledWith('USER', 'student-1',
      { email: 'nuevo@cidemags.edu', firstNames: 'Ana', lastNames: 'Lopez' });
  });

  it('Eliminar solicita el borrado logico', () => {
    const fixture = TestBed.createComponent(DataRectificationComponent);
    fixture.componentRef.setInput('entityId', 'student-1');
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Eliminar').click();

    expect(softDeleteData).toHaveBeenCalledWith('USER', 'student-1');
  });
});
