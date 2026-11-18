import { Injectable } from '@angular/core';
import {
  ConsentFormItem,
  LegalNoticeContent,
} from '../../domain/models/legal/legal.model';
import {
  LegalDocument,
  LegalDocumentId,
} from '../../domain/models/legal/legal-document.model';

/**
 * Contenido legal de la plataforma (specs 36-50).
 *
 * Los mismos textos se publican como Markdown en `docs/legal/`; si cambias uno,
 * actualiza el otro y la fecha de `updatedAt` de ambos.
 */
@Injectable({ providedIn: 'root' })
export class LegalContentService {
  private readonly updatedAt = '2026-11-18';

  private readonly documents: Record<LegalDocumentId, LegalDocument> = {
    'aviso-legal': {
      id: 'aviso-legal',
      title: 'Aviso legal',
      updatedAt: this.updatedAt,
      intro:
        'Este aviso legal regula el acceso y el uso de la plataforma CIDEM Integrity, en cumplimiento del artículo 10 de la Ley 34/2002 de Servicios de la Sociedad de la Información y de Comercio Electrónico (LSSI-CE).',
      sections: [
        {
          heading: '1. Titular del sitio',
          items: [
            'Denominación: CIDEMags S.L. (en adelante, «CIDEM»).',
            'Domicilio social: pendiente de publicación por el titular.',
            'Correo electrónico de contacto: dpo@cidemags.edu.',
            'Actividad: desarrollo y explotación de una plataforma tecnológica de integridad académica.',
          ],
        },
        {
          heading: '2. Objeto',
          paragraphs: [
            'La plataforma permite a centros educativos crear exámenes, aplicarlos en línea y supervisar la integridad académica durante su realización. El acceso a las funciones de examen y supervisión requiere una cuenta proporcionada por el centro.',
            'La navegación por los documentos legales publicados es libre y no requiere registro.',
          ],
        },
        {
          heading: '3. Propiedad intelectual e industrial',
          items: [
            'El código fuente de este proyecto se distribuye bajo la licencia MIT incluida en el repositorio.',
            'La marca CIDEM, el logotipo, el diseño de la interfaz, los textos legales y la documentación son propiedad de CIDEM o de sus licenciantes y no están cubiertos por la licencia MIT.',
            'Queda prohibida la reproducción, distribución o transformación de los elementos anteriores sin autorización expresa, salvo los usos permitidos por la ley.',
          ],
        },
        {
          heading: '4. Condiciones de uso',
          items: [
            'El usuario se compromete a facilitar información veraz y a mantener la confidencialidad de sus credenciales.',
            'No está permitido utilizar la plataforma para fines ilícitos, fraudulentos o contrarios a la buena fe.',
            'El centro educativo es responsable de la configuración de los exámenes y de informar a su alumnado sobre las condiciones de aplicación.',
          ],
        },
        {
          heading: '5. Exclusión de responsabilidad',
          paragraphs: [
            'La plataforma se ofrece en su versión alpha, «tal cual» y sin garantías de disponibilidad continua. CIDEM no responde de las interrupciones derivadas de mantenimientos, fallos de conectividad o causas de fuerza mayor.',
            'CIDEM no controla los contenidos de sitios de terceros a los que se pueda enlazar desde la documentación.',
          ],
        },
        {
          heading: '6. Legislación aplicable y jurisdicción',
          paragraphs: [
            'Este aviso legal se rige por la legislación española. Cualquier controversia se someterá a los juzgados y tribunales del domicilio del titular, salvo que la normativa de consumo establezca otro fuero.',
          ],
        },
        {
          heading: '7. Contacto',
          paragraphs: [
            'Para cualquier consulta sobre este aviso legal puedes escribir a dpo@cidemags.edu.',
          ],
        },
      ],
    },

    privacidad: {
      id: 'privacidad',
      title: 'Política de privacidad',
      updatedAt: this.updatedAt,
      intro:
        'Esta política explica qué datos personales trata CIDEM Integrity, con qué finalidad, durante cuánto tiempo y cómo puedes ejercer tus derechos conforme al Reglamento (UE) 2016/679 (RGPD) y a la Ley Orgánica 3/2018 (LOPDGDD).',
      sections: [
        {
          heading: '1. Responsable del tratamiento',
          items: [
            'Responsable: CIDEMags S.L. (en adelante, «CIDEM»).',
            'Domicilio social: pendiente de publicación por el titular.',
            'Correo electrónico: dpo@cidemags.edu.',
            'Delegado de Protección de Datos: dpo@cidemags.edu.',
          ],
        },
        {
          heading: '2. Datos que tratamos',
          items: [
            'Datos identificativos: nombre, apellidos, correo electrónico y rol (alumno, profesor o administrativo).',
            'Credenciales: contraseña almacenada exclusivamente como hash bcrypt; nunca en texto plano.',
            'Datos académicos: exámenes, preguntas, respuestas, intentos, calificaciones y resultados.',
            'Datos de integridad: eventos de foco y cambio de pestaña, latidos de sesión, alertas y puntuaciones de riesgo.',
            'Datos opcionales de supervisión: cámara, micrófono y capturas de pantalla, solo si el estudiante otorga su consentimiento expreso.',
            'Datos técnicos: dirección IP, agente de usuario y marcas de tiempo; la auditoría los pseudonimiza cuando deja de ser necesario identificarlos.',
          ],
        },
        {
          heading: '3. Finalidades y bases jurídicas',
          items: [
            'Prestación del servicio educativo y gestión de la cuenta: ejecución de la relación contractual o precontractual (art. 6.1.b RGPD).',
            'Aplicación de exámenes y verificación de la integridad académica: interés legítimo del centro educativo (art. 6.1.f RGPD).',
            'Cámara, micrófono y capturas de pantalla: consentimiento explícito del estudiante (art. 6.1.a RGPD), revocable en cualquier momento.',
            'Cumplimiento de obligaciones legales y atención de reclamaciones: obligación legal (art. 6.1.c RGPD) e interés legítimo (art. 6.1.f RGPD).',
          ],
        },
        {
          heading: '4. Consentimiento para funciones de supervisión',
          paragraphs: [
            'Las funciones de cámara, micrófono y captura de pantalla son opcionales y se activan únicamente con el consentimiento previo del estudiante. La retirada del consentimiento no afecta a la licitud del tratamiento realizado antes de retirarlo.',
          ],
        },
        {
          heading: '5. Conservación',
          items: [
            'Datos académicos y resultados: tres años desde la finalización del examen, salvo plazos legales superiores.',
            'Eventos de integridad y alertas: el tiempo necesario para su revisión y, después, pseudonimizados o suprimidos.',
            'Registros de auditoría: se conservan pseudonimizados para garantizar la trazabilidad del sistema.',
            'Credenciales y sesiones: mientras la cuenta esté activa y hasta la revocación del token correspondiente.',
          ],
        },
        {
          heading: '6. Destinatarios y encargados',
          paragraphs: [
            'No se ceden datos a terceros salvo obligación legal. Los proveedores de alojamiento y de base de datos actúan como encargados del tratamiento con un contrato que garantiza la confidencialidad y la seguridad.',
            'El personal del centro con rol administrativo o docente puede consultar los datos estrictamente necesarios para gestionar exámenes y revisiones.',
          ],
        },
        {
          heading: '7. Transferencias internacionales',
          paragraphs: [
            'La plataforma se despliega en infraestructura propia o contratada dentro del Espacio Económico Europeo. No se realizan transferencias internacionales sin las garantías previstas en el capítulo V del RGPD.',
          ],
        },
        {
          heading: '8. Derechos de las personas',
          items: [
            'Acceso a los datos personales que tratamos.',
            'Rectificación de datos inexactos o incompletos.',
            'Supresión (derecho al olvido) cuando ya no sean necesarios.',
            'Portabilidad de los datos en formato estructurado.',
            'Limitación del tratamiento en los supuestos legales.',
            'Oposición al tratamiento basado en interés legítimo.',
            'Retirada del consentimiento en cualquier momento.',
          ],
        },
        {
          heading: '9. Cómo ejercer tus derechos',
          paragraphs: [
            'Puedes ejercerlos desde el centro de privacidad de la aplicación o escribiendo a dpo@cidemags.edu. Responderemos en el plazo máximo de un mes, prorrogable otros dos en casos complejos.',
            'Si consideras que no hemos atendido correctamente tu solicitud, puedes reclamar ante la Agencia Española de Protección de Datos (www.aepd.es).',
          ],
        },
        {
          heading: '10. Menores de edad',
          paragraphs: [
            'Si el estudiante es menor de 14 años, el consentimiento para el tratamiento de sus datos deberá prestarlo quien ostente la patria potestad o tutela. El centro educativo es responsable de verificar esta circunstancia.',
          ],
        },
        {
          heading: '11. Medidas de seguridad',
          items: [
            'Cifrado de las comunicaciones mediante HTTPS.',
            'Contraseñas almacenadas con bcrypt y tokens de sesión de corta duración.',
            'Control de acceso por roles y registro de auditoría de las acciones sensibles.',
            'Pseudonimización de direcciones IP y datos de auditoría cuando dejan de ser necesarios.',
          ],
        },
        {
          heading: '12. Cambios en esta política',
          paragraphs: [
            'Podemos actualizar esta política para adaptarla a cambios normativos o funcionales. La fecha de última actualización aparece al inicio del documento y los cambios sustanciales se comunicarán en la plataforma.',
          ],
        },
      ],
    },

    cookies: {
      id: 'cookies',
      title: 'Política de cookies',
      updatedAt: this.updatedAt,
      intro:
        'CIDEM Integrity no utiliza cookies publicitarias, de analítica ni de perfilado. Solo emplea almacenamiento local estrictamente necesario para mantener la sesión autenticada.',
      sections: [
        {
          heading: '1. Qué son las cookies y el almacenamiento local',
          paragraphs: [
            'Las cookies son pequeños archivos que un sitio web guarda en el navegador. El almacenamiento local (localStorage e IndexedDB) cumple una función similar, pero no se envía automáticamente al servidor en cada petición.',
          ],
        },
        {
          heading: '2. Cookies que utilizamos',
          paragraphs: [
            'Ninguna. La plataforma no instala cookies propias ni de terceros, no usa Google Analytics ni herramientas equivalentes y no comparte identificadores con redes publicitarias.',
          ],
        },
        {
          heading: '3. Almacenamiento local estrictamente necesario',
          items: [
            'auth_access_token: token de acceso JWT que identifica la sesión. Duración: 15 minutos; se renueva mientras la sesión está activa.',
            'auth_refresh_token: token para renovar el acceso sin volver a introducir la contraseña. Duración: 7 días o hasta el cierre de sesión.',
            'Ionic Storage: réplica de los tokens en IndexedDB (web) o almacenamiento nativo (aplicación de escritorio), para que la sesión sobreviva al cierre de la pestaña.',
          ],
        },
        {
          heading: '4. Base legal',
          paragraphs: [
            'Este almacenamiento está exento del deber de consentimiento previo porque es técnicamente imprescindible para prestar el servicio solicitado (artículo 22.2 de la LSSI-CE). Por ese motivo no mostramos un banner de cookies: no hay cookies opcionales que aceptar.',
          ],
        },
        {
          heading: '5. Cómo eliminarlo',
          paragraphs: [
            'Puedes borrar estos datos desde la configuración de tu navegador (Privacidad → Borrar datos de navegación). Si lo haces, se cerrará la sesión y tendrás que iniciar sesión de nuevo. En la aplicación de escritorio, cerrar sesión elimina los tokens almacenados.',
          ],
        },
        {
          heading: '6. Recursos de terceros',
          paragraphs: [
            'Las tipografías, iconos y hojas de estilo se empaquetan con la propia aplicación; no se cargan desde CDN externas. El backend solo se contacta con la API configurada para la plataforma.',
          ],
        },
        {
          heading: '7. Actualizaciones',
          paragraphs: [
            'Si en el futuro incorporamos cookies opcionales, actualizaremos esta política y solicitaremos el consentimiento previo antes de instalarlas.',
          ],
        },
      ],
    },

    terminos: {
      id: 'terminos',
      title: 'Términos de uso',
      updatedAt: this.updatedAt,
      intro:
        'Estos términos regulan el uso de CIDEM Integrity por parte de estudiantes, profesorado y personal administrativo del centro educativo.',
      sections: [
        {
          heading: '1. Aceptación',
          paragraphs: [
            'Al crear una cuenta o utilizar la plataforma aceptas estos términos, la política de privacidad y la política de cookies. Si no estás de acuerdo, no utilices el servicio.',
          ],
        },
        {
          heading: '2. Descripción del servicio',
          paragraphs: [
            'CIDEM Integrity permite crear y aplicar exámenes en línea, registrar eventos de integridad y generar alertas para su revisión. La plataforma se encuentra en fase alpha y puede cambiar sin previo aviso.',
          ],
        },
        {
          heading: '3. Cuenta de usuario',
          items: [
            'Debes proporcionar datos veraces y mantenerlos actualizados.',
            'Las credenciales son personales e intransferibles; no las compartas.',
            'Debes comunicar de inmediato cualquier uso no autorizado de tu cuenta.',
            'El centro puede suspender cuentas que incumplan estos términos.',
          ],
        },
        {
          heading: '4. Uso aceptable',
          items: [
            'No suplantar la identidad de otra persona ni falsear la autoría de un examen.',
            'No automatizar el registro, el acceso o la resolución de exámenes.',
            'No intentar eludir los controles de integridad, alterar los eventos registrados o manipular las alertas.',
            'No extraer, copiar ni redistribuir exámenes o datos de otras personas.',
            'No introducir código malicioso ni realizar ataques de denegación de servicio.',
          ],
        },
        {
          heading: '5. Exámenes y supervisión',
          paragraphs: [
            'Durante un examen la plataforma puede registrar eventos de foco, cambios de pestaña y latidos de sesión. La cámara, el micrófono y las capturas de pantalla solo se activan con consentimiento expreso y pueden desactivarse antes de comenzar.',
            'El estudiantado debe conocer las condiciones de aplicación que establezca su centro antes de iniciar el examen.',
          ],
        },
        {
          heading: '6. Propiedad intelectual y licencia',
          paragraphs: [
            'El código fuente se distribuye bajo la licencia MIT. La marca, el diseño, los textos legales y los contenidos de los exámenes pertenecen a CIDEM o al centro educativo correspondiente y no pueden reutilizarse sin autorización.',
          ],
        },
        {
          heading: '7. Disponibilidad y garantías',
          paragraphs: [
            'El servicio se presta «tal cual» y «según disponibilidad». No garantizamos ausencia de interrupciones, errores o pérdida de datos más allá de las copias de seguridad programadas.',
          ],
        },
        {
          heading: '8. Limitación de responsabilidad',
          paragraphs: [
            'En la medida permitida por la ley, CIDEM no responde de daños indirectos, lucro cesante o pérdida de datos derivados del uso de la plataforma. Nada en estos términos limita los derechos que la normativa de consumo reconozca al usuario.',
          ],
        },
        {
          heading: '9. Suspensión y cancelación',
          paragraphs: [
            'Podemos suspender o cancelar el acceso ante un uso fraudulento, un riesgo de seguridad o el cierre del centro en la plataforma. Puedes solicitar la supresión de tu cuenta desde el centro de privacidad.',
          ],
        },
        {
          heading: '10. Modificaciones',
          paragraphs: [
            'Podemos modificar estos términos para adaptarlos a cambios legales o funcionales. La versión vigente y su fecha de actualización estarán siempre disponibles en esta página.',
          ],
        },
        {
          heading: '11. Ley aplicable y jurisdicción',
          paragraphs: [
            'Estos términos se rigen por la legislación española. Las controversias se someterán a los juzgados y tribunales del domicilio del titular, salvo las reglas imperativas de consumo.',
          ],
        },
        {
          heading: '12. Contacto',
          paragraphs: [
            'Para dudas sobre estos términos escribe a dpo@cidemags.edu.',
          ],
        },
      ],
    },
  };

  private readonly order: LegalDocumentId[] = [
    'aviso-legal',
    'privacidad',
    'cookies',
    'terminos',
  ];

  /** Documentos legales públicos en el orden en que se enlazan. */
  getDocuments(): LegalDocument[] {
    return this.order.map((id) => this.documents[id]);
  }

  /** Devuelve un documento por id o `null` si no existe. */
  getDocument(id: string | null | undefined): LegalDocument | null {
    if (!id || !(id in this.documents)) {
      return null;
    }
    return this.documents[id as LegalDocumentId];
  }

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

  /** Resumen en texto plano de la política de privacidad (compatibilidad). */
  getPrivacyPolicy(): string {
    const doc = this.documents.privacidad;
    const lines: string[] = [doc.title.toUpperCase(), `Ultima actualizacion: ${doc.updatedAt}`];

    for (const section of doc.sections) {
      lines.push('', section.heading);
      lines.push(...(section.paragraphs ?? []));
      lines.push(...(section.items ?? []));
    }

    return lines.join('\n');
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
