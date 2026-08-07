import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorEarning, AmbassadorEarningsHistoryResponse, AmbassadorEarningsSummary } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-earnings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6">
      <div>
        <h2 class="text-2xl font-semibold">Earnings</h2>
        <p class="text-sm text-slate-400">Rewards calculated from your referrals that reached a qualifying status.</p>
      </div>

      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4" *ngIf="summary">
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4">
          <p class="text-sm text-slate-400">Paid out</p>
          <p class="mt-2 text-2xl font-semibold text-emerald-300">{{ formatAmount(summary.totalPaid) }}</p>
        </div>
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4">
          <p class="text-sm text-slate-400">Approved (awaiting payout)</p>
          <p class="mt-2 text-2xl font-semibold">{{ formatAmount(summary.totalApproved) }}</p>
        </div>
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4">
          <p class="text-sm text-slate-400">Pending / eligible</p>
          <p class="mt-2 text-2xl font-semibold text-amber-300">{{ formatAmount(summary.totalPendingOrEligible) }}</p>
        </div>
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4">
          <p class="text-sm text-slate-400">Rejected / reversed</p>
          <p class="mt-2 text-2xl font-semibold text-slate-500">{{ formatAmount(summary.totalRejectedOrReversed) }}</p>
        </div>
      </div>

      <div class="rounded-xl border border-slate-800" *ngIf="history">
        <div class="border-b border-slate-800 px-4 py-3">
          <h3 class="font-medium">Reward history</h3>
        </div>
        <div *ngIf="history.rewards.length; else noRewards">
          <div class="grid grid-cols-[1.5fr_1fr_1fr_1fr_1fr] gap-4 px-4 py-3 text-xs uppercase tracking-wide text-slate-500">
            <span>Campaign</span>
            <span>Referral</span>
            <span>Amount</span>
            <span>Status</span>
            <span>Created</span>
          </div>
          <div
            *ngFor="let reward of history.rewards"
            class="grid grid-cols-[1.5fr_1fr_1fr_1fr_1fr] gap-4 border-t border-slate-800 px-4 py-3 text-sm"
          >
            <span>{{ reward.campaignName }}</span>
            <span class="text-slate-400">{{ reward.referralCode }}</span>
            <span>{{ reward.rewardValue }} {{ reward.currency || '' }}</span>
            <span [class]="statusClass(reward)">{{ reward.status }}<ng-container *ngIf="reward.holdReason"> ({{ reward.holdReason }})</ng-container></span>
            <span class="text-slate-400">{{ reward.createdAt | date: 'mediumDate' }}</span>
          </div>
        </div>
        <ng-template #noRewards>
          <p class="px-4 py-6 text-sm text-slate-400">No rewards yet - they appear once a referral you brought in reaches a qualifying status.</p>
        </ng-template>
      </div>

      <div class="flex items-center justify-between text-sm text-slate-400" *ngIf="history && history.totalPages > 1">
        <button
          type="button"
          class="rounded-md border border-slate-700 px-3 py-1.5 disabled:opacity-40"
          [disabled]="history.page <= 0"
          (click)="loadHistory(history.page - 1)"
        >
          Previous
        </button>
        <span>Page {{ history.page + 1 }} of {{ history.totalPages }}</span>
        <button
          type="button"
          class="rounded-md border border-slate-700 px-3 py-1.5 disabled:opacity-40"
          [disabled]="history.page + 1 >= history.totalPages"
          (click)="loadHistory(history.page + 1)"
        >
          Next
        </button>
      </div>
    </div>
  `
})
export class AmbassadorEarningsComponent implements OnInit {
  summary: AmbassadorEarningsSummary | null = null;
  history: AmbassadorEarningsHistoryResponse | null = null;
  errorMessage = '';

  constructor(private ambassadorPortalService: AmbassadorPortalService) {}

  ngOnInit(): void {
    this.ambassadorPortalService
      .getEarningsSummary()
      .subscribe({
        next: summary => (this.summary = summary),
        error: error => (this.errorMessage = extractApiErrorMessage(error, 'Unable to load earnings summary.'))
      });
    this.loadHistory(0);
  }

  loadHistory(page: number): void {
    this.ambassadorPortalService
      .listEarnings(page, 20)
      .pipe(finalize(() => {}))
      .subscribe({
        next: history => (this.history = history),
        error: error => (this.errorMessage = extractApiErrorMessage(error, 'Unable to load earnings history.'))
      });
  }

  formatAmount(value: number): string {
    return (value ?? 0).toFixed(2) + (this.summary?.currency ? ' ' + this.summary.currency : '');
  }

  statusClass(reward: AmbassadorEarning): string {
    switch (reward.status) {
      case 'PAID':
        return 'text-emerald-300';
      case 'APPROVED':
      case 'ELIGIBLE':
        return 'text-cyan-300';
      case 'PENDING':
        return 'text-amber-300';
      default:
        return 'text-slate-500';
    }
  }
}
