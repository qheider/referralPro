import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../shared/models/auth.model';

interface NavigationItem {
  label: string;
  route?: string;
  description: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.css'
})
export class DashboardLayoutComponent implements OnInit {
  currentUser: User | null = null;
  mobileSidebarOpen = false;
  currentPageTitle = 'Overview';

  readonly navigationItems: NavigationItem[] = [
    {
      label: 'Overview',
      route: '/dashboard/overview',
      description: 'Dashboard landing page'
    },
    {
      label: 'Campaigns',
      description: 'Detailed campaign views in phase 7',
      disabled: true
    },
    {
      label: 'Analytics',
      description: 'Deep analytics routes in phase 7',
      disabled: true
    },
    {
      label: 'Rewards',
      description: 'Reward reporting in phase 7',
      disabled: true
    }
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

    if (this.authService.isAuthenticated()) {
      this.authService.getCurrentUser().subscribe();
    }

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => {
        const navigationEvent = event as NavigationEnd;
        this.syncPageTitle(navigationEvent.urlAfterRedirects);
        this.mobileSidebarOpen = false;
      });
  }

  toggleSidebar(): void {
    this.mobileSidebarOpen = !this.mobileSidebarOpen;
  }

  closeSidebar(): void {
    this.mobileSidebarOpen = false;
  }

  logout(): void {
    this.mobileSidebarOpen = false;
    this.authService.logout();
  }

  getUserInitials(): string {
    if (!this.currentUser?.username) {
      return 'RP';
    }

    return this.currentUser.username
      .split('@')[0]
      .split(/[.\-_]/)
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  }

  private syncPageTitle(url: string): void {
    const activeItem = this.navigationItems.find(item => item.route && url.startsWith(item.route));
    this.currentPageTitle = activeItem?.label ?? 'Overview';
  }
}
