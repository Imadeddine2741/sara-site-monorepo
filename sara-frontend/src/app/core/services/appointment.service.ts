import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppointmentRequest, AppointmentResponse } from '../models/appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {

  private readonly apiUrl = 'http://localhost:8080/api/appointments';

  constructor(private http: HttpClient) {}

  create(appointment: AppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(this.apiUrl, appointment);
  }

  getMyAppointments(): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${this.apiUrl}/me`);
  }

  cancel(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${id}/cancel`);
  }
}
