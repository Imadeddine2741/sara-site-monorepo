import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {

  loading = false;
  message?: string;
  error?: string;
  form: FormGroup;

  private readonly apiUrl = 'http://localhost:8080/api/auth';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.message = undefined;
    this.error = undefined;

    this.http.post(`${this.apiUrl}/forgot-password`, this.form.value, { responseType: 'text' })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.message = response;
        },
        error: (err) => {
          this.loading = false;
          this.error = err.error || 'Une erreur est survenue.';
        }
      });
  }
}