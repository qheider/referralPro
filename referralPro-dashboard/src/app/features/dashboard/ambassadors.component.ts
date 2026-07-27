import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AmbassadorAdminService } from '../../core/services/ambassador-admin.service';
import { AmbassadorPageResponse, AmbassadorSummary } from '../../shared/models/ambassador.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassadors',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col gap-4 rounded-3xl bg-white p-6 shadow-sm lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-wide text-indigo-600">Admin</p>
          <h2 class="mt-1 text-2xl font-semibold text-slate-900">Ambassadors</h2>
          <p class="mt-2 text-sm text-slate-500">Create, activate, and monitor ambassadors for your company.</p>
        </div>

        <a
          routerLink="/dashboard/ambassadors/new"
          class="inline-flex items-center justify-center rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
        >
          New ambassador
        </a>
      </div>

      <div class="grid gap-4 rounded-3xl bg-white p-6 shadow-sm md:grid-cols-4">
        <label class="space-y-2 md:col-span-2">
          <span class="text-sm font-medium text-slate-700">Search</span>
          <input
            [(ngModel)]="search"
            type="text"
            class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"
            placeholder="Name, email, or display name"
            (keyup.enter)="applyFilters()"
          />
        </label>

        <label class="space-y-2">
          <span class="text-sm font-medium text-slate-700">Status</span>
          <select
            [(ngModel)]="status"
            class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          >
            <option value="">All</option>
            <option value="INVITED">Invited</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="SUSPENDED">Suspended</option>
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

      <div *ngIf="errorMessage" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {{ errorMessage }}
      </div>

      <div *ngIf="isLoading" class="rounded-3xl bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
        Loading ambassadors...
      </div>

      <div *ngIf="!isLoading && page as currentPage" class="overflow-hidden rounded-3xl bg-white shadow-sm">
        <div *ngIf="currentPage.content.length === 0" class="p-10 text-center text-sm text-slate-500">
          No ambassadors matched the current filters.
        </div>

        <div *ngIf="currentPage.content.length > 0">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-slate-200">
            <thead class="bg-slate-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Ambassador</th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Status</th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Campaigns</th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Registrations</th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Rentals</th>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Conversion</th>
                <th class="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr *ngFor="let ambassador of currentPage.content; trackBy: trackByAmbassadorId">
                <td class="px-6 py-4">
                  <div class="font-semibold text-slate-900">{{ ambassador.firstName }} {{ ambassador.lastName }}</div>
                  <div class="text-sm text-slate-500">{{ ambassador.email }}</div>
                  <div class="text-xs text-slate-400" *ngIf="ambassador.displayName">{{ ambassador.displayName }}</div>
                </td>
                <td class="px-6 py-4">
                  <span class="rounded-full px-2.5 py-1 text-xs font-semibold"
                    [class.bg-emerald-100]="ambassador.status === 'ACTIVE'"
                    [class.text-emerald-700]="ambassador.status === 'ACTIVE'"
                    [class.bg-amber-100]="ambassador.status === 'INVITED'"
                    [class.text-amber-700]="ambassador.status === 'INVITED'"
                    [class.bg-slate-100]="ambassador.status === 'INACTIVE' || ambassador.status === 'SUSPENDED'"
                    [class.text-slate-700]="ambassador.status === 'INACTIVE' || ambassador.status === 'SUSPENDED'">
                    {{ ambassador.status }}
                  </span>
                </td>
                <td class="px-6 py-4 text-sm text-slate-700">{{ ambassador.assignedCampaigns }}</td>
                <td class="px-6 py-4 text-sm text-slate-700">{{ ambassador.totalRegistrations }}</td>
                <td class="px-6 py-4 text-sm text-slate-700">{{ ambassador.successfulRentals }}</td>
                <td class="px-6 py-4 text-sm text-slate-700">{{ ambassador.conversionRate | number:'1.0-2' }}%</td>
                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end gap-2">
                    <a
                      [routerLink]="['/dashboard/ambassadors', ambassador.id]"
                      class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                    >
                      View
                    </a>
                    <button
                      *ngIf="ambassador.status !== 'ACTIVE'"
                      type="button"
                      class="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-500"
                      (click)="activate(ambassador)"
                    >
                      Activate
                    </button>
                    <button
                      *ngIf="ambassador.status === 'ACTIVE'"
                      type="button"
                      class="rounded-lg bg-slate-900 px-3 py-2 text-xs font-semibold text-white hover:bg-slate-700"
                      (click)="deactivate(ambassador)"
                    >
                      Deactivate
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between border-t border-slate-200 px-6 py-4 text-sm text-slate-500">
          <span>Showing {{ currentPage.content.length }} of {{ currentPage.totalElements }} ambassadors</span>
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
export class AmbassadorsComponent implements OnInit {
  page: AmbassadorPageResponse | null = null;
  isLoading = false;
  errorMessage = '';
  search = '';
  status = '';
  currentPage = 0;

  constructor(private ambassadorAdminService: AmbassadorAdminService) {}

  ngOnInit(): void {
    this.loadAmbassadors();
  }

  loadAmbassadors(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.ambassadorAdminService
      .listAmbassadors({
        page: this.currentPage,
        search: this.search.trim() || undefined,
        status: this.status || undefined,
        sort: 'createdAt,desc'
      })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: page => {
          this.page = page;
          this.currentPage = page.page;
        },
        error: error => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load ambassadors.');
          this.page = null;
        }
      });
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadAmbassadors();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadAmbassadors();
  }

  activate(ambassador: AmbassadorSummary): void {
    this.ambassadorAdminService.activateAmbassador(ambassador.id).subscribe({
      next: () => this.loadAmbassadors(),
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to activate ambassador.');
      }
    });
  }

  deactivate(ambassador: AmbassadorSummary): void {
    this.ambassadorAdminService.deactivateAmbassador(ambassador.id).subscribe({
      next: () => this.loadAmbassadors(),
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to deactivate ambassador.');
      }
    });
  }

  trackByAmbassadorId(_index: number, ambassador: AmbassadorSummary): number {
    return ambassador.id;
  }
}
