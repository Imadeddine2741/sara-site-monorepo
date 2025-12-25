import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { RegisterRequest } from '../../../core/models/register-request.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {

  loading = false;
  error?: string;
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router          // <== injection du Router
  ) {
    this.form = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.error = undefined;

    this.authService.register(this.form.value as RegisterRequest).subscribe({
      next: () => {
        this.loading = false;
        // Afficher un message type :
        this.error = undefined;
        alert('Compte créé. Un email de confirmation vous a été envoyé.');
        this.router.navigate(['/login']);
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Erreur lors de la création du compte';
        console.error(err);
      }
    });
  }
}
