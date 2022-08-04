import {Directive, ElementRef, Output, EventEmitter, HostListener} from '@angular/core';

@Directive({
  selector: '[datalabClickedOutside]'
})
export class UpdatedClickedOutsideDirective {
  counter = 0;
  constructor(private el: ElementRef) { }

  @Output() public clickedOutside = new EventEmitter();

  @HostListener('document:click', ['$event.target'])
  public onClick(target: any) {
    this.counter ++;
    const clickedInside = this.el.nativeElement.contains(target);
    if (!clickedInside && this.counter > 1) {
      this.clickedOutside.emit(target);
    }
  }
}
