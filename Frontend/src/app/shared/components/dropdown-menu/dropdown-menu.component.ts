import { Component, HostListener, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'gt-dropdown-menu',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="dropdown-container">
      <button class="btn btn-sm btn-ghost trigger" (click)="toggle($event)">
        <gt-icon name="more-horizontal" [size]="16" />
      </button>
      <div class="dropdown-menu" [class.show]="isOpen">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [
    `
      .dropdown-container {
        position: relative;
        display: inline-block;
      }
      .trigger {
        padding: 4px;
        line-height: 1;
        border-radius: 50%;
      }
      .dropdown-menu {
        display: none;
        position: absolute;
        right: 0;
        top: 100%;
        min-width: 160px;
        background: #fff;
        border: 1px solid var(--gt-border);
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        z-index: 1000;
        padding: 4px 0;
        margin-top: 4px;
      }
      .dropdown-menu.show {
        display: block;
      }
      ::ng-deep .dropdown-menu .dropdown-item,
      ::ng-deep .dropdown-menu button {
        display: block;
        width: 100%;
        text-align: left;
        padding: 8px 16px;
        background: transparent;
        border: none;
        color: var(--gt-text);
        font-size: 14px;
        cursor: pointer;
      }
      ::ng-deep .dropdown-menu .dropdown-item:hover,
      ::ng-deep .dropdown-menu button:hover {
        background: var(--gt-gray-100);
      }
      ::ng-deep .dropdown-menu .dropdown-item.text-danger,
      ::ng-deep .dropdown-menu button.text-danger {
        color: var(--gt-red);
      }
    `,
  ],
})
export class DropdownMenuComponent {
  isOpen = false;

  toggle(event: Event) {
    event.stopPropagation();
    this.isOpen = !this.isOpen;
  }

  @HostListener('document:click', ['$event'])
  closeDropdown(event: Event) {
    this.isOpen = false;
  }
}
