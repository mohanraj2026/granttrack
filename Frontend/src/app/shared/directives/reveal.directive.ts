import { AfterViewInit, Directive, ElementRef, Input, OnDestroy, inject } from '@angular/core';

/**
 * Reveal-on-scroll: fades + slides an element into view the first time it
 * enters the viewport. Usage: <div gtReveal [revealDelay]="100">…</div>
 */
@Directive({
  selector: '[gtReveal]',
  standalone: true,
})
export class RevealDirective implements AfterViewInit, OnDestroy {
  private el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  /** Stagger delay in ms. */
  @Input() revealDelay = 0;

  ngAfterViewInit(): void {
    const node = this.el.nativeElement;
    node.classList.add('gt-reveal');
    node.style.transitionDelay = `${this.revealDelay}ms`;

    if (!('IntersectionObserver' in window)) {
      node.classList.add('gt-reveal-in');
      return;
    }
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            node.classList.add('gt-reveal-in');
            this.observer?.unobserve(node);
          }
        }
      },
      { threshold: 0.12 },
    );
    this.observer.observe(node);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
