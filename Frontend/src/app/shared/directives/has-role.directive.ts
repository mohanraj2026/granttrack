import { Directive, Input, TemplateRef, ViewContainerRef, effect, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

/**
 * Structural directive: render content only if the current user has one of the given roles.
 * Usage: <button *gtHasRole="['ROLE_ADMIN','ROLE_GRANT_ADMIN']">…</button>
 */
@Directive({
  selector: '[gtHasRole]',
  standalone: true,
})
export class HasRoleDirective {
  private tpl = inject(TemplateRef<unknown>);
  private vcr = inject(ViewContainerRef);
  private auth = inject(AuthService);

  private roles: string[] = [];
  private shown = false;

  @Input({ required: true })
  set gtHasRole(roles: string[] | string) {
    this.roles = Array.isArray(roles) ? roles : [roles];
    this.update();
  }

  constructor() {
    // Re-evaluate whenever the authenticated user changes.
    effect(() => {
      this.auth.currentUser();
      this.update();
    });
  }

  private update(): void {
    const allowed = this.auth.hasAnyRole(this.roles);
    if (allowed && !this.shown) {
      this.vcr.createEmbeddedView(this.tpl);
      this.shown = true;
    } else if (!allowed && this.shown) {
      this.vcr.clear();
      this.shown = false;
    }
  }
}
