import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CompanyIntegrationService } from '../../core/services/company-integration.service';
import { WebhookEventDetailResponse } from '../../shared/models/company-integration.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-integration-webhook-event-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './integration-webhook-event-detail.component.html',
  styleUrl: './integration-webhook-event-detail.component.css'
})
export class IntegrationWebhookEventDetailComponent implements OnInit {
  event: WebhookEventDetailResponse | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private companyIntegrationService: CompanyIntegrationService
  ) {}

  ngOnInit(): void {
    const webhookEventId = Number(this.route.snapshot.paramMap.get('webhookEventId'));
    if (!Number.isInteger(webhookEventId) || webhookEventId <= 0) {
      this.errorMessage = 'Invalid webhook event id.';
      return;
    }

    this.isLoading = true;
    this.companyIntegrationService
      .getWebhookEvent(webhookEventId)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: event => {
          this.event = event;
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load webhook event detail.');
        }
      });
  }

  get formattedPayload(): string {
    if (!this.event) {
      return '';
    }
    try {
      return JSON.stringify(JSON.parse(this.event.rawPayload), null, 2);
    } catch {
      return this.event.rawPayload;
    }
  }
}
