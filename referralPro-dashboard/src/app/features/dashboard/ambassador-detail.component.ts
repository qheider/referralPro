import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AmbassadorAdminService } from '../../core/services/ambassador-admin.service';
import { AmbassadorDetail } from '../../shared/models/ambassador.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section *ngIf="ambassador" class="space-y-6">
      <div class="rounded-3xl bg-white p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p class="text-sm font-semibold uppercase tracking-wide text-indigo-600">Ambassador detail</p>
            <h2 class="mt-1 text-2xl font-semibold text-slate-900">{{ ambassador.firstName }} {{ ambassador.lastName }}</h2>
            <p class="mt-2 text-sm text-slate-500">{{ ambassador.email }}</p>
            <p class="mt-1 text-sm text-slate-500" *ngIf="ambassador.displayName">{{ ambassador.displayName }}</p>
          </div>

          <div class="flex flex-wrap gap-3">
            <a [routerLink]="['/dashboard/ambassadors', ambassador.id, 'edit']" class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Edit</a>
            <button *ngIf="ambassador.status !== 'ACTIVE'" type="button" class="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500" (click)="activate()">Activate</button>
            <button *ngIf="ambassador.status === 'ACTIVE'" type="button" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700" (click)="deactivate()">Deactivate</button>
          </div>
        </div>
      </div>

      <div *ngIf="errorMessage" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {{ errorMessage }}
      </div>

      <div class="grid gap-4 md:grid-cols-4">
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Status</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.status }}</p>
        </div>
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Assigned campaigns</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.assignedCampaigns }}</p>
        </div>
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Registrations</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.totalRegistrations }}</p>
        </div>
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Successful rentals</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.successfulRentals }}</p>
        </div>
      </div>

      <div class="grid gap-6 lg:grid-cols-[2fr,1fr]">
        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <h3 class="text-lg font-semibold text-slate-900">Profile</h3>
          <dl class="mt-4 grid gap-4 md:grid-cols-2">
            <div>
              <dt class="text-xs font-semibold uppercase tracking-wide text-slate-500">Ambassador code</dt>
              <dd class="mt-1 text-sm text-slate-900">{{ ambassador.ambassadorCode }}</dd>
            </div>
            <div>
              <dt class="text-xs font-semibold uppercase tracking-wide text-slate-500">Phone</dt>
              <dd class="mt-1 text-sm text-slate-900">{{ ambassador.phone || 'Not provided' }}</dd>
            </div>
            <div>
              <dt class="text-xs font-semibold uppercase tracking-wide text-slate-500">Social platform</dt>
              <dd class="mt-1 text-sm text-slate-900">{{ ambassador.socialMediaPlatform || 'Not provided' }}</dd>
            </div>
            <div>
              <dt class="text-xs font-semibold uppercase tracking-wide text-slate-500">Social handle</dt>
              <dd class="mt-1 text-sm text-slate-900">{{ ambassador.socialMediaHandle || 'Not provided' }}</dd>
            </div>
            <div class="md:col-span-2">
              <dt class="text-xs font-semibold uppercase tracking-wide text-slate-500">Bio</dt>
              <dd class="mt-1 text-sm text-slate-900">{{ ambassador.bio || 'No bio added yet.' }}</dd>
            </div>
          </dl>
        </div>

        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <h3 class="text-lg font-semibold text-slate-900">Referral links</h3>
          <div class="mt-4 space-y-3" *ngIf="ambassador.referralLinks.length; else noLinks">
            <div *ngFor="let link of ambassador.referralLinks" class="rounded-2xl border border-slate-200 p-4">
              <p class="text-sm font-semibold text-slate-900">{{ link.campaignName }}</p>
              <p class="mt-1 break-all text-xs text-slate-500">{{ link.publicToken }}</p>
              <p class="mt-2 text-xs text-slate-500">Clicks: {{ link.clickCount }} · {{ link.status }}</p>
            </div>
          </div>
          <ng-template #noLinks>
            <p class="mt-4 text-sm text-slate-500">No referral links have been generated yet.</p>
          </ng-template>
        </div>
      </div>
    </section>

    <div *ngIf="!ambassador && !errorMessage" class="rounded-3xl bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      Loading ambassador...
    </div>
  `
})
export class AmbassadorDetailComponent implements OnInit {
  ambassador: AmbassadorDetail | null = null;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private ambassadorAdminService: AmbassadorAdminService
  ) {}

  ngOnInit(): void {
    this.loadAmbassador();
  }

  activate(): void {
    if (!this.ambassador) {
      return;
    }

    this.ambassadorAdminService.activateAmbassador(this.ambassador.id).subscribe({
      next: ambassador => (this.ambassador = ambassador),
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to activate ambassador.');
      }
    });
  }

  deactivate(): void {
    if (!this.ambassador) {
      return;
    }

    this.ambassadorAdminService.deactivateAmbassador(this.ambassador.id).subscribe({
      next: ambassador => (this.ambassador = ambassador),
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to deactivate ambassador.');
      }
    });
  }

  private loadAmbassador(): void {
    const ambassadorId = Number(this.route.snapshot.paramMap.get('ambassadorId'));
    this.ambassadorAdminService.getAmbassador(ambassadorId).subscribe({
      next: ambassador => {
        this.ambassador = ambassador;
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load ambassador.');
      }
    });
  }
}
