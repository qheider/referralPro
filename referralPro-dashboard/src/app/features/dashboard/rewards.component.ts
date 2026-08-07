import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs';
import { RevenueService } from '../../core/services/revenue.service';
import { AmbassadorRewardResponse, AmbassadorRewardStatus } from '../../shared/models/revenue.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

const STATUS_FILTERS: (AmbassadorRewardStatus | null)[] = [null, 'PENDING', 'ELIGIBLE', 'APPROVED', 'PAID', 'REJECTED', 'REVERSED'];

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.css'
})
export class RewardsComponent implements OnInit {
  readonly statusFilters = STATUS_FILTERS;

  rewards: AmbassadorRewardResponse[] = [];
  selectedStatus: AmbassadorRewardStatus | null = null;
  isLoading = false;
  errorMessage = '';
  actionInFlightId: number | null = null;

  constructor(private revenueService: RevenueService) {}

  ngOnInit(): void {
    this.load();
  }

  selectStatus(status: AmbassadorRewardStatus | null): void {
    this.selectedStatus = status;
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.revenueService
      .listRewards({ status: this.selectedStatus })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: rewards => (this.rewards = rewards),
        error: (error: unknown) => (this.errorMessage = extractApiErrorMessage(error, 'Unable to load rewards.'))
      });
  }

  approve(reward: AmbassadorRewardResponse): void {
    this.runAction(reward, this.revenueService.approveReward(reward.id));
  }

  markPaid(reward: AmbassadorRewardResponse): void {
    this.runAction(reward, this.revenueService.markRewardPaid(reward.id));
  }

  reject(reward: AmbassadorRewardResponse): void {
    const reason = window.prompt('Reason for rejecting this reward?');
    if (!reason) {
      return;
    }
    this.runAction(reward, this.revenueService.rejectReward(reward.id, reason));
  }

  private runAction(reward: AmbassadorRewardResponse, action: ReturnType<RevenueService['approveReward']>): void {
    this.actionInFlightId = reward.id;
    this.errorMessage = '';
    action.pipe(finalize(() => (this.actionInFlightId = null))).subscribe({
      next: updated => {
        const index = this.rewards.findIndex(r => r.id === updated.id);
        if (index >= 0) {
          this.rewards[index] = updated;
        }
      },
      error: (error: unknown) => (this.errorMessage = extractApiErrorMessage(error, 'Unable to update reward.'))
    });
  }

  statusBadgeClass(status: AmbassadorRewardStatus): string {
    switch (status) {
      case 'PAID':
        return 'bg-emerald-50 text-emerald-700';
      case 'APPROVED':
        return 'bg-cyan-50 text-cyan-700';
      case 'ELIGIBLE':
        return 'bg-indigo-50 text-indigo-700';
      case 'REJECTED':
      case 'REVERSED':
        return 'bg-red-50 text-red-700';
      default:
        return 'bg-amber-50 text-amber-700';
    }
  }
}
