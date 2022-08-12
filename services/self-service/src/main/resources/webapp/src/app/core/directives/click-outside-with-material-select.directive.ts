import { Directive, ElementRef, Output, EventEmitter, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[datalabClickedOutsideMatSelect]'
})
export class ClickedOutsideMatSelectDirective {
  constructor(private el: ElementRef) { }
  @Input() isFormOpened: boolean;
  @Output() public clickedOutside = new EventEmitter();

  @HostListener('document:click', ['$event.target'])
  public onClick(target: any): void {
    if (!this.isFormOpened) {
      return;
    }
    const clickedInside = this.el.nativeElement.contains(target);
    const isClickOnDropdown = [...target.classList].some(item => item.includes('mat-option'));
    const isClickOnOverlay = [...target.classList].some(item => item.includes('cdk-overlay'));
    const isClickOnForm = !clickedInside && !isClickOnDropdown && !isClickOnOverlay;

    if (isClickOnForm) {
      this.clickedOutside.emit(target);
    }
  }
}
