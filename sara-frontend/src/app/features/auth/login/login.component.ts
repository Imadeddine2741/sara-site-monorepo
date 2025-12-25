import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  loading = false;
  error?: string;
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router         // <== injection du Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading = true;
    this.error = undefined;

    this.authService.login(this.form.value as any).subscribe({
      next: () => {
        this.loading = false;
        // redirection après login
        this.router.navigate(['/home']);
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Email ou mot de passe incorrect';
        console.error(err);
      }
    });
  }
}
