import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonIcon } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { desktopOutline } from 'ionicons/icons';

/**
 * Pantalla de bloqueo (spec 51): se muestra cuando un usuario intenta
 * iniciar un examen que exige la app de escritorio desde un navegador.
 * No ofrece acciones alternativas: el único camino es abrir la app CIDEM.
 */
@Component({
  selector: 'app-exam-desktop-required',
  template: `
    <section class="desktop-required" role="alert">
      <ion-icon name="desktop-outline" aria-hidden="true"></ion-icon>
      <h2>Aplicación de escritorio requerida</h2>
      <p>
        Este examen solo puede realizarse desde la aplicación de escritorio
        CIDEM por cuestiones de seguridad. Inicia sesión desde la app para
        continuarlo.
      </p>
    </section>
  `,
  styles: [
    `
      .desktop-required {
        max-width: 34rem;
        margin: 3rem auto;
        padding: 2rem;
        text-align: center;
        border: 1px solid var(--bs-border-color, #dee2e6);
        border-radius: 0.75rem;
      }
      ion-icon {
        font-size: 2.5rem;
        margin-bottom: 0.5rem;
      }
      h2 {
        font-size: 1.25rem;
        margin-bottom: 0.75rem;
      }
    `,
  ],
  imports: [CommonModule, IonIcon],
})
export class ExamDesktopRequiredComponent {
  constructor() {
    addIcons({ desktopOutline });
  }
}
