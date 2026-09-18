import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonList,
  IonItem,
  IonLabel,
  IonButton,
  IonIcon,
  IonFab,
  IonFabButton,
  IonSpinner,
  AlertController,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { addOutline, createOutline, trashOutline } from 'ionicons/icons';
import { ListExamsUseCase } from '../../../application/use-cases/exam/list-exams.use-case';
import { DeleteExamUseCase } from '../../../application/use-cases/exam/delete-exam.use-case';
import { Exam } from '../../../domain/models/exam/exam.model';

@Component({
  selector: 'app-exam-list',
  templateUrl: 'exam-list.page.html',
  styleUrls: ['exam-list.page.scss'],
  imports: [
    CommonModule,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonContent,
    IonList,
    IonItem,
    IonLabel,
    IonButton,
    IonIcon,
    IonFab,
    IonFabButton,
    IonSpinner,
  ],
})
export class ExamListPage implements OnInit {
  exams: Exam[] = [];
  loading = true;

  constructor(
    private readonly listExamsUseCase: ListExamsUseCase,
    private readonly deleteExamUseCase: DeleteExamUseCase,
    private readonly alertController: AlertController,
  ) {
    addIcons({ addOutline, createOutline, trashOutline });
  }

  ngOnInit(): void {
    this.loadExams();
  }

  loadExams(): void {
    this.loading = true;
    this.listExamsUseCase.execute().subscribe((exams) => {
      this.exams = exams;
      this.loading = false;
    });
  }

  async confirmDelete(exam: Exam): Promise<void> {
    const alert = await this.alertController.create({
      header: 'Eliminar examen',
      message: `¿Seguro que quieres eliminar "${exam.title}"? Esta acción no se puede deshacer.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Eliminar', role: 'destructive', handler: () => this.deleteExam(exam.id) },
      ],
    });
    await alert.present();
  }

  private deleteExam(id: string): void {
    this.deleteExamUseCase.execute(id).subscribe(() => this.loadExams());
  }
}
