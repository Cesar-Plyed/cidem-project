import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AccessRequestComponent } from './access-request.component';
import { LegalApiService } from '../../../core/services/legal-api.service';

function buttonByText(root: HTMLElement, text: string): HTMLButtonElement {
  const button = Array.from(root.querySelectorAll('button'))
    .find((candidate) => candidate.textContent?.includes(text));
  if (!button) {
    throw new Error(`Boton no encontrado: ${text}`);
  }
  return button as HTMLButtonElement;
}

describe('AccessRequestComponent', () => {
  let getAccess: ReturnType<typeof vi.fn>;
  let exportAccess: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getAccess = vi.fn(() => of({ personalData: {}, responses: [], alerts: [], auditTrail: [] }));
    exportAccess = vi.fn(() => of(new Blob(['{"personalData":{}}'], { type: 'application/json' })));

    TestBed.configureTestingModule({
      imports: [AccessRequestComponent],
      providers: [{ provide: LegalApiService, useValue: { getAccess, exportAccess } }],
    });
  });

  it('Solicitar copia muestra las secciones recibidas', () => {
    const fixture = TestBed.createComponent(AccessRequestComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Solicitar copia').click();
    fixture.detectChanges();

    expect(getAccess).toHaveBeenCalledWith('student-1');
    expect(fixture.componentInstance.sections()).toContain('personalData');
  });

  it('Exportar genera un enlace de descarga con tamano y fecha', () => {
    const fixture = TestBed.createComponent(AccessRequestComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Exportar a JSON').click();
    fixture.detectChanges();

    const artifact = fixture.componentInstance.lastExport();
    expect(exportAccess).toHaveBeenCalledWith('student-1');
    expect(artifact).not.toBeNull();
    expect(artifact!.url).toBeTruthy();
    expect(artifact!.size).toBeGreaterThan(0);
    expect(artifact!.generatedAt).toBeTruthy();
  });
});
