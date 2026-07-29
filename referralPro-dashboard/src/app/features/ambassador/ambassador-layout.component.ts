import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../shared/models/auth.model';

@Component({
  selector: 'app-ambassador-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-slate-950 text-slate-100">
      <header class="border-b border-slate-800 bg-slate-900/80">
        <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <div>
            <p class="text-xs uppercase tracking-[0.3em] text-cyan-400">Ambassador portal</p>
            <h1 class="text-xl font-semibold">{{ currentPageTitle }}</h1>
          </div>
          <div class="flex items-center gap-3">
            <span class="rounded-full bg-cyan-500/20 px-3 py-1 text-sm text-cyan-200">{{ currentUser?.username }}</span>
            <button type="button" class="rounded-md border border-slate-700 px-3 py-2 text-sm" (click)="logout()">Logout</button>
          </div>
        </div>
      </header>

      <div class="mx-auto grid max-w-6xl gap-6 px-6 py-6 md:grid-cols-[220px_1fr]">
        <nav class="space-y-2 rounded-xl border border-slate-800 bg-slate-900 p-4">
          <a *ngFor="let item of navItems"
             [routerLink]="item.route"
             routerLinkActive="bg-cyan-500/20 text-cyan-200"
             class="block rounded-lg px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800 hover:text-white">
            {{ item.label }}
          </a>
        </nav>

        <main class="rounded-xl border border-slate-800 bg-slate-900 p-6">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class AmbassadorLayoutComponent implements OnInit {
  currentUser: User | null = null;
  currentPageTitle = 'Overview';

  readonly navItems = [
    { label: 'Overview', route: '/ambassador/overview' },
    { label: 'Campaigns', route: '/ambassador/campaigns' },
    { label: 'Referrals', route: '/ambassador/referrals' },
    { label: 'Analytics', route: '/ambassador/analytics' },
    { label: 'Profile', route: '/ambassador/profile' }
  ];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUserValue();
    this.syncPageTitle(this.router.url);

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => {
        this.syncPageTitle((event as NavigationEnd).urlAfterRedirects);
      });
  }

  logout(): void {
    this.authService.logout();
  }

  private syncPageTitle(url: string): void {
    if (url.includes('/ambassador/campaigns/')) {
      this.currentPageTitle = 'Campaign detail';
      return;
    }

    const active = this.navItems.find(item => url.startsWith(item.route));
    this.currentPageTitle = active?.label ?? 'Overview';
  }
}
