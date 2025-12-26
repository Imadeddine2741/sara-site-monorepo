import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AppointmentService } from '../core/services/appointment.service';
import { AppointmentRequest } from '../core/models/appointment.model';

@Component({
  selector: 'app-new-appointment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './new-appointment.component.html',
  styleUrls: ['./new-appointment.component.scss']
})
export class NewAppointmentComponent {

  // Formulaire
  selectedDate: string = '';
  selectedTime: string = '';
  motif: string = '';

  // États
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // Contraintes
  minDate: string;
  availableTimes: string[] = [
    '09:00', '10:00', '11:00',
    '14:00', '15:00', '16:00', '17:00'
  ];

  constructor(
    private appointmentService: AppointmentService,
    private router: Router
  ) {
    // Date minimum = demain
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.minDate = tomorrow.toISOString().split('T')[0];
  }

  onSubmit() {
    if (!this.selectedDate || !this.selectedTime || !this.motif.trim()) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const dateHeure = `${this.selectedDate}T${this.selectedTime}:00`;

    const request: AppointmentRequest = {
      dateHeure: dateHeure,
      motif: this.motif.trim()
    };

    this.appointmentService.create(request).subscribe({
      next: () => {
        this.successMessage = 'Rendez-vous réservé avec succès !';
        setTimeout(() => {
          this.router.navigate(['/my-appointments']);
        }, 1500);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Erreur lors de la réservation';
      }
    });
  }

  isWeekend(dateStr: string): boolean {
    const date = new Date(dateStr);
    const day = date.getDay();
    return day === 0 || day === 6;
  }
}