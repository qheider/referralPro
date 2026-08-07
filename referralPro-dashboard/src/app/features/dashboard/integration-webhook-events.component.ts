import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CompanyIntegrationService } from '../../core/services/company-integration.service';
import { WebhookEventStatus, WebhookEventSummaryResponse } from '../../shared/models/company-integration.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

const STATUS_FILTERS: (WebhookEventStatus | null)[] = [
  null,
  'RECEIVED',
  'PROCESSING',
  'RETRY_SCHEDULED',
  'PROCESSED',
  'IGNORED',
  'MANUAL_REVIEW'
];

@Component({
  selector: 'app-integration-webhook-events',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './integration-webhook-events.component.html',
  styleUrl: './integration-webhook-events.component.css'
})
export class IntegrationWebhookEventsComponent implements OnInit {
  readonly statusFilters = STATUS_FILTERS;

  events: WebhookEventSummaryResponse[] = [];
  selectedStatus: WebhookEventStatus | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(private companyIntegrationService: CompanyIntegrationService) {}

  ngOnInit(): void {
    this.load();
  }

  selectStatus(status: WebhookEventStatus | null): void {
    this.selectedStatus = status;
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.companyIntegrationService
      .listWebhookEvents(this.selectedStatus)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: events => {
          this.events = events;
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load webhook events.');
        }
      });
  }

  statusBadgeClass(status: WebhookEventStatus): string {
    switch (status) {
      case 'PROCESSED':
        return 'bg-emerald-50 text-emerald-700';
      case 'MANUAL_REVIEW':
        return 'bg-red-50 text-red-700';
      case 'RETRY_SCHEDULED':
        return 'bg-amber-50 text-amber-700';
      case 'IGNORED':
        return 'bg-slate-100 text-slate-500';
      default:
        return 'bg-indigo-50 text-indigo-700';
    }
  }
}
