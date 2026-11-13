import { Injectable } from '@angular/core';
import {
  ConsentFormItem,
  LegalNoticeContent,
} from '../../domain/models/legal/legal.model';

/** Contenido legal obligatorio (Art. 13-14 GDPR / LOPDGDD). Spec 37. */
@Injectable({ providedIn: 'root' })
export class LegalContentService {
  private readonly updatedAt = '2026-11-13';

  getLegalNotice(): LegalNoticeContent {
    return {
      title: 'Informacion sobre proteccion de datos',
      responsible: 'CIDEMags S.L.',
      purpose: 'Monitorizacion de integridad academica durante la realizacion de examenes.',
      legalBasis:
        'Consentimiento explicito del estudiante (Art. 6.1.a GDPR) e interes legitimo del centro educativo en garantizar la integridad del examen (Art. 6.1.f GDPR).',
      rights: [
        'Acceso a tus datos personales',
        'Rectificacion de datos inexactos',
        'Supresion (derecho al olvido)',
        'Portabilidad de tus datos',
        'Limitacion del tratamiento',
        'Oposicion al tratamiento',
      ],
      contact: 'dpo@cidemags.edu',
      dpo: 'Delegado de Proteccion de Datos: dpo@cidemags.edu',
      articles: [
        'Art. 13 GDPR / LOPDGDD: informacion facilitada al obtener los datos.',
        'Art. 14 GDPR / LOPDGDD: informacion cuando los datos no se obtienen del interesado.',
      ],
      updatedAt: this.updatedAt,
    };
  }

  getPrivacyPolicy(): string {
    return [
      'POLITICA DE PRIVACIDAD',
      'Responsable: CIDEMags S.L.',
      'Finalidad: verificacion de integridad academica durante examenes.',
      'Datos tratados: identificacion, respuestas del examen, eventos de foco/pestana y alertas.',
      'Base legal: consentimiento explicito e interes legitimo del centro.',
      'Conservacion: 3 anos desde la finalizacion del examen.',
      'Derechos: acceso, rectificacion, supresion, portabilidad, limitacion y oposicion.',
      'Contacto DPO: dpo@cidemags.edu',
      'Reclamaciones: Agencia Espanola de Proteccion de Datos (www.aepd.es).',
    ].join('\n');
  }

  getConsentForm(): ConsentFormItem[] {
    return [
      {
        type: 'CAMERA',
        label: 'Camara',
        description: 'Captura puntual de imagen para verificar la identidad y presencia del alumno.',
        required: false,
      },
      {
        type: 'MICROPHONE',
        label: 'Microfono',
        description: 'Deteccion de ruido ambiental durante el examen.',
        required: false,
      },
      {
        type: 'SCREENSHOT',
        label: 'Capturas de pantalla',
        description: 'Capturas periodicas de la pantalla del dispositivo del examen.',
        required: false,
      },
      {
        type: 'TAB_MONITORING',
        label: 'Monitorizacion de pestanas',
        description: 'Registro de cambios de pestana o perdida de foco de la ventana del examen.',
        required: false,
      },
    ];
  }
}
