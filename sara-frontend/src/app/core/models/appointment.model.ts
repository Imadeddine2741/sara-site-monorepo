export interface AppointmentRequest {
  dateHeure: string;
  motif: string;
}

export interface AppointmentResponse {
  id: number;
  dateHeure: string;
  motif: string;
  patientNom: string;
  patientPrenom: string;
  status: 'PLANNED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  canPatientCancel: boolean;
}
