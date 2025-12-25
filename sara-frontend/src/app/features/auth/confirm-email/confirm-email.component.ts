import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-confirm-email',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './confirm-email.component.html',
  styleUrls: ['./confirm-email.component.scss']
})
export class ConfirmEmailComponent implements OnInit {

  loading = true;
  message = '';
  success = false;

  private readonly apiUrl = 'http://localhost:8080/api/auth';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.loading = false;
      this.message = 'Token manquant dans l\'URL.';
      this.success = false;
      return;
    }

    this.http.get(`${this.apiUrl}/confirm-email?token=${token}`, { responseType: 'text' })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.message = response;
          this.success = true;
        },
        error: (err) => {
          this.loading = false;
          this.message = err.error || 'Erreur lors de la confirmation.';
          this.success = false;
        }
      });
  }
}