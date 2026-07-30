import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { IconComponent, IconName } from '../../shared/components/icon/icon.component';
import { RevealDirective } from '../../shared/directives/reveal.directive';

interface Feature { icon: IconName; title: string; text: string; }
interface RoleCard { icon: IconName; name: string; text: string; }

@Component({
  selector: 'gt-landing',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, IconComponent, RevealDirective],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent {
  private auth = inject(AuthService);
  readonly isAuthed = this.auth.isAuthenticated;

  readonly stats = [
    { value: '9', label: 'Integrated modules' },
    { value: '6', label: 'Role-based workspaces' },
    { value: '100%', label: 'Auditable decisions' },
    { value: 'End-to-end', label: 'Grant lifecycle' },
  ];

  readonly features: Feature[] = [
    { icon: 'wallet', title: 'Funding & Calls', text: 'Configure funding schemes, award ranges and open submission windows.' },
    { icon: 'file-text', title: 'Applications', text: 'Guided multi-step submission with team, budget and proposal in one place.' },
    { icon: 'scale', title: 'Blind Peer Review', text: 'Conflict screening, structured scoring rubrics and panel consensus decisions.' },
    { icon: 'award', title: 'Award Management', text: 'Issue grant letters and track each award through its full lifecycle.' },
    { icon: 'landmark', title: 'Milestone Disbursement', text: 'Release funds in stages against approved, evidenced milestones.' },
    { icon: 'trending-up', title: 'Progress Tracking', text: 'Periodic reports and deliverables reviewed by compliance officers.' },
    { icon: 'book', title: 'Research Outputs', text: 'Record publications, patents, datasets and open-access compliance.' },
    { icon: 'shield-check', title: 'Security & Audit', text: 'JWT auth, role-based access control and full audit trails on every decision.' },
  ];

  readonly roles: RoleCard[] = [
    { icon: 'file-text', name: 'Principal Investigator', text: 'Submit applications, build teams and report on progress.' },
    { icon: 'scale', name: 'Peer Reviewer', text: 'Evaluate assigned proposals under blind review conditions.' },
    { icon: 'layers', name: 'Grant Administrator', text: 'Assign reviewers, run panels and issue awards.' },
    { icon: 'landmark', name: 'Finance Officer', text: 'Approve milestones and release grant funds.' },
    { icon: 'shield-check', name: 'Compliance Officer', text: 'Review deliverables and monitor adherence.' },
    { icon: 'settings', name: 'Research Admin', text: 'Configure schemes, calls and platform users.' },
  ];

  readonly lifecycle: { icon: IconName; label: string }[] = [
    { icon: 'wallet', label: 'Funding' },
    { icon: 'file-text', label: 'Apply' },
    { icon: 'scale', label: 'Review' },
    { icon: 'award', label: 'Award' },
    { icon: 'landmark', label: 'Disburse' },
    { icon: 'trending-up', label: 'Progress' },
    { icon: 'book', label: 'Outputs' },
  ];
}
