import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {

  loading = false;
  message?: string;
  error?: string;
  success = false;
  token?: string;
  form: FormGroup;

  private readonly apiUrl = 'http://localhost:8080/api/auth';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || undefined;

    if (!this.token) {
      this.error = 'Token manquant dans l\'URL.';
    }
  }

  onSubmit(): void {
    if (this.form.invalid || !this.token) {
      return;
    }

    const { newPassword, confirmPassword } = this.form.value;

    if (newPassword !== confirmPassword) {
      this.error = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;
    this.message = undefined;
    this.error = undefined;

    const payload = {
      token: this.token,
      newPassword: newPassword
    };

    this.http.post(`${this.apiUrl}/reset-password`, payload, { responseType: 'text' })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.message = response;
          this.success = true;
        },
        error: (err) => {
          this.loading = false;
          this.error = err.error || 'Une erreur est survenue.';
        }
      });
  }
}