import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AppointmentService } from '../core/services/appointment.service';
import { AppointmentResponse } from '../core/models/appointment.model';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  templateUrl: './my-appointments.component.html',
  styleUrls: ['./my-appointments.component.scss']
})
export class MyAppointmentsComponent implements OnInit {

  appointments: AppointmentResponse[] = [];
  loading = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit() {
    this.loadAppointments();
  }

  loadAppointments() {
    this.loading = true;
    this.errorMessage = null;
    this.appointmentService.getMyAppointments().subscribe({
      next: (appointments) => {
        this.appointments = appointments;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors du chargement des rendez-vous';
        this.loading = false;
      }
    });
  }

  cancelAppointment(id: number) {
    if (confirm('Confirmer l\'annulation du rendez-vous ?')) {
      this.appointmentService.cancel(id).subscribe({
        next: (response) => {
          this.successMessage = response.message || 'RDV annulé avec succès !';
          this.loadAppointments();
          setTimeout(() => this.successMessage = null, 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Erreur lors de l\'annulation';
          setTimeout(() => this.errorMessage = null, 5000);
        }
      });
    }
  }

  daysBefore(dateStr: string): number {
    const date = new Date(dateStr);
    const diffTime = date.getTime() - Date.now();
    const days = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return Math.max(0, days);
  }
}
