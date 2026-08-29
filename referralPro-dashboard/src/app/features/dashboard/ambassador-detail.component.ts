import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AmbassadorAdminService } from '../../core/services/ambassador-admin.service';
import { AuthService } from '../../core/services/auth.service';
import { AmbassadorDetail, AmbassadorReferralLink } from '../../shared/models/ambassador.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';
import { ReferralQrCodeComponent } from '../../shared/components/referral-qr-code.component';

@Component({
  selector: 'app-ambassador-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReferralQrCodeComponent],
  template: `
    <div *ngIf="errorMessage()" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {{ errorMessage() }}
    </div>

    <section *ngIf="ambassador() as ambassador" class="space-y-6">
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

      <div class="grid gap-4 md:grid-cols-4">
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Status</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.status }}</p>
        </div>
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Assigned campaigns</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.assignedCampaigns ?? 0 }}</p>
        </div>
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Registrations</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.totalRegistrations ?? 0 }}</p>
        </div>
        <div class="rounded-3xl bg-white p-5 shadow-sm">
          <p class="text-sm text-slate-500">Successful rentals</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">{{ ambassador.successfulRentals ?? 0 }}</p>
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
              <p class="mt-1 break-all text-xs text-slate-500">{{ link.referralUrl || link.publicToken }}</p>
              <p class="mt-2 text-xs text-slate-500">Clicks: {{ link.clickCount }} · {{ link.status }}</p>
              <app-referral-qr-code
                *ngIf="link.qrCodeUrl"
                class="mt-3 block"
                theme="light"
                [qrCodeUrl]="link.qrCodeUrl"
                [fileName]="downloadFileName(link)"
              />
            </div>
          </div>
          <ng-template #noLinks>
            <p class="mt-4 text-sm text-slate-500">No referral links have been generated yet.</p>
          </ng-template>
        </div>
      </div>
    </section>

    <div *ngIf="!ambassador() && !errorMessage()" class="rounded-3xl bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
      Loading ambassador...
    </div>
  `
})
export class AmbassadorDetailComponent implements OnInit {
  readonly ambassador = signal<AmbassadorDetail | null>(null);
  readonly errorMessage = signal('');

  constructor(
    private route: ActivatedRoute,
    private ambassadorAdminService: AmbassadorAdminService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAmbassador();
  }

  activate(): void {
    const current = this.ambassador();
    if (!current) {
      return;
    }

    this.ambassadorAdminService.activateAmbassador(current.id).subscribe({
      next: ambassador => this.applyAmbassador(ambassador),
      error: error => {
        this.errorMessage.set(extractApiErrorMessage(error, 'Unable to activate ambassador.'));
      }
    });
  }

  deactivate(): void {
    const current = this.ambassador();
    if (!current) {
      return;
    }

    this.ambassadorAdminService.deactivateAmbassador(current.id).subscribe({
      next: ambassador => this.applyAmbassador(ambassador),
      error: error => {
        this.errorMessage.set(extractApiErrorMessage(error, 'Unable to deactivate ambassador.'));
      }
    });
  }

  private loadAmbassador(): void {
    const rawId = this.route.snapshot.paramMap.get('ambassadorId');
    const ambassadorId = Number(rawId);
    if (!rawId || Number.isNaN(ambassadorId)) {
      console.error('AmbassadorDetailComponent: missing/invalid ambassadorId route param', rawId);
      this.errorMessage.set('Invalid ambassador link.');
      return;
    }

    this.ambassadorAdminService.getAmbassador(ambassadorId).subscribe({
      next: ambassador => this.applyAmbassador(ambassador),
      error: error => {
        this.errorMessage.set(extractApiErrorMessage(error, 'Unable to load ambassador.'));
      }
    });
  }

  private applyAmbassador(ambassador: AmbassadorDetail): void {
    this.ambassador.set({ ...ambassador, referralLinks: ambassador.referralLinks ?? [] });
    this.errorMessage.set('');
  }

  // The QR image itself already has the company name and campaign name printed as a header
  // (see AmbassadorAdminService.toReferralLinkResponse / ReferralRedirectController's
  // withHeader=true route) - this just carries the same identification into the saved filename.
  downloadFileName(link: AmbassadorReferralLink): string {
    const companyName = this.authService.currentUserSignal()?.companyName || 'ReferralPro';
    const parts = [companyName, link.campaignName, 'QR'].map(part => this.slugify(part));
    return `${parts.join('-')}.png`;
  }

  private slugify(value: string): string {
    return value
      .trim()
      .replace(/[^a-zA-Z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '') || 'referral';
  }
}
