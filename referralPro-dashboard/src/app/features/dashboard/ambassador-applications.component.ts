import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AmbassadorApplicationAdminService } from '../../core/services/ambassador-application-admin.service';
import {
  AmbassadorApplicationPageResponse,
  AmbassadorApplicationSummary
} from '../../shared/models/ambassador-application.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-applications',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col gap-4 rounded-3xl bg-white p-6 shadow-sm lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-wide text-indigo-600">Admin</p>
          <h2 class="mt-1 text-2xl font-semibold text-slate-900">Ambassador applications</h2>
          <p class="mt-2 text-sm text-slate-500">Review and approve or reject people who applied to become ambassadors.</p>
        </div>

        <a
          routerLink="/dashboard/ambassadors"
          class="inline-flex items-center justify-center rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
        >
          Back to ambassadors
        </a>
      </div>

      <div class="grid gap-4 rounded-3xl bg-white p-6 shadow-sm md:grid-cols-4">
        <label class="space-y-2 md:col-span-2">
          <span class="text-sm font-medium text-slate-700">Search</span>
          <input
            [ngModel]="search()"
            (ngModelChange)="search.set($event)"
            type="text"
            class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"
            placeholder="Name or email"
            (keyup.enter)="applyFilters()"
          />
        </label>

        <label class="space-y-2">
          <span class="text-sm font-medium text-slate-700">Status</span>
          <select
            [ngModel]="status()"
            (ngModelChange)="onStatusChange($event)"
            class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          >
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="">All</option>
          </select>
        </label>

        <button
          type="button"
          class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500"
          (click)="applyFilters()"
        >
          Apply filters
        </button>
      </div>

      <div *ngIf="errorMessage()" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {{ errorMessage() }}
      </div>

      <div *ngIf="isLoading()" class="rounded-3xl bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
        Loading applications...
      </div>

      <div *ngIf="!isLoading() && page() as currentPage" class="overflow-hidden rounded-3xl bg-white shadow-sm">
        <div *ngIf="currentPage.content.length === 0" class="p-10 text-center text-sm text-slate-500">
          No applications matched the current filters.
        </div>

        <div *ngIf="currentPage.content.length > 0">
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-slate-200">
              <thead class="bg-slate-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Applicant</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Campaign</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Submitted</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Status</th>
                  <th class="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <ng-container *ngFor="let application of currentPage.content; trackBy: trackByApplicationId">
                  <tr>
                    <td class="px-6 py-4">
                      <div class="font-semibold text-slate-900">{{ application.firstName }} {{ application.lastName }}</div>
                      <div class="text-sm text-slate-500">{{ application.email }}</div>
                    </td>
                    <td class="px-6 py-4 text-sm text-slate-700">{{ application.campaignName || 'General application' }}</td>
                    <td class="px-6 py-4 text-sm text-slate-700">{{ application.submittedAt | date:'mediumDate' }}</td>
                    <td class="px-6 py-4">
                      <span class="rounded-full px-2.5 py-1 text-xs font-semibold"
                        [class.bg-amber-100]="application.status === 'PENDING'"
                        [class.text-amber-700]="application.status === 'PENDING'"
                        [class.bg-emerald-100]="application.status === 'APPROVED'"
                        [class.text-emerald-700]="application.status === 'APPROVED'"
                        [class.bg-red-100]="application.status === 'REJECTED'"
                        [class.text-red-700]="application.status === 'REJECTED'">
                        {{ application.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-right">
                      <div class="flex justify-end gap-2" *ngIf="application.status === 'PENDING'">
                        <button
                          type="button"
                          class="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50"
                          [disabled]="isBusy(application.id)"
                          (click)="approve(application)"
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          class="rounded-lg bg-red-600 px-3 py-2 text-xs font-semibold text-white hover:bg-red-500 disabled:opacity-50"
                          [disabled]="isBusy(application.id)"
                          (click)="startReject(application)"
                        >
                          Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr *ngIf="rejectingId() === application.id">
                    <td colspan="5" class="bg-slate-50 px-6 py-4">
                      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
                        <input
                          [(ngModel)]="rejectionReason"
                          type="text"
                          maxlength="500"
                          placeholder="Reason for rejecting this application"
                          class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"
                        />
                        <div class="flex gap-2">
                          <button
                            type="button"
                            class="rounded-lg bg-red-600 px-3 py-2 text-xs font-semibold text-white hover:bg-red-500 disabled:opacity-50"
                            [disabled]="!rejectionReason.trim() || isBusy(application.id)"
                            (click)="confirmReject(application)"
                          >
                            Confirm reject
                          </button>
                          <button
                            type="button"
                            class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-white"
                            (click)="cancelReject()"
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    </td>
                  </tr>
                </ng-container>
              </tbody>
            </table>
          </div>

          <div class="flex items-center justify-between border-t border-slate-200 px-6 py-4 text-sm text-slate-500">
            <span>Showing {{ currentPage.content.length }} of {{ currentPage.totalElements }} applications</span>
            <div class="flex items-center gap-2">
              <button type="button" class="rounded-lg border border-slate-200 px-3 py-1.5 disabled:opacity-40" [disabled]="currentPage.first" (click)="goToPage(currentPage.page - 1)">Previous</button>
              <span>Page {{ currentPage.page + 1 }} of {{ currentPage.totalPages || 1 }}</span>
              <button type="button" class="rounded-lg border border-slate-200 px-3 py-1.5 disabled:opacity-40" [disabled]="currentPage.last" (click)="goToPage(currentPage.page + 1)">Next</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  `
})
export class AmbassadorApplicationsComponent implements OnInit {
  readonly page = signal<AmbassadorApplicationPageResponse | null>(null);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly search = signal('');
  readonly status = signal('PENDING');
  readonly rejectingId = signal<number | null>(null);
  readonly busyId = signal<number | null>(null);

  rejectionReason = '';

  private pageIndex = 0;
  // Guards against a slower earlier response landing after a newer one and overwriting the
  // list with results for a filter the user has already moved off of - same pattern as
  // AmbassadorsComponent.
  private latestRequestId = 0;

  constructor(private ambassadorApplicationAdminService: AmbassadorApplicationAdminService) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    const requestId = ++this.latestRequestId;
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.ambassadorApplicationAdminService
      .listApplications({
        page: this.pageIndex,
        search: this.search().trim() || undefined,
        status: this.status() || undefined,
        sort: 'createdAt,desc'
      })
      .pipe(
        finalize(() => {
          if (requestId === this.latestRequestId) {
            this.isLoading.set(false);
          }
        })
      )
      .subscribe({
        next: page => {
          if (requestId !== this.latestRequestId) {
            return;
          }

          this.page.set({ ...page, content: page.content ?? [] });
          this.pageIndex = page.page;
        },
        error: error => {
          if (requestId !== this.latestRequestId) {
            return;
          }

          this.errorMessage.set(extractApiErrorMessage(error, 'Unable to load ambassador applications.'));
          this.page.set(null);
        }
      });
  }

  goToPage(page: number): void {
    this.pageIndex = page;
    this.loadApplications();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.loadApplications();
  }

  onStatusChange(status: string): void {
    this.status.set(status);
    this.applyFilters();
  }

  isBusy(applicationId: number): boolean {
    return this.busyId() === applicationId;
  }

  approve(application: AmbassadorApplicationSummary): void {
    this.busyId.set(application.id);
    this.errorMessage.set('');

    this.ambassadorApplicationAdminService
      .approveApplication(application.id)
      .pipe(finalize(() => this.busyId.set(null)))
      .subscribe({
        next: () => this.loadApplications(),
        error: error => {
          this.errorMessage.set(extractApiErrorMessage(error, 'Unable to approve application.'));
        }
      });
  }

  startReject(application: AmbassadorApplicationSummary): void {
    this.rejectingId.set(application.id);
    this.rejectionReason = '';
  }

  cancelReject(): void {
    this.rejectingId.set(null);
    this.rejectionReason = '';
  }

  confirmReject(application: AmbassadorApplicationSummary): void {
    const reason = this.rejectionReason.trim();
    if (!reason) {
      return;
    }

    this.busyId.set(application.id);
    this.errorMessage.set('');

    this.ambassadorApplicationAdminService
      .rejectApplication(application.id, { reason })
      .pipe(finalize(() => this.busyId.set(null)))
      .subscribe({
        next: () => {
          this.rejectingId.set(null);
          this.rejectionReason = '';
          this.loadApplications();
        },
        error: error => {
          this.errorMessage.set(extractApiErrorMessage(error, 'Unable to reject application.'));
        }
      });
  }

  trackByApplicationId(_index: number, application: AmbassadorApplicationSummary): number {
    return application.id;
  }
}
