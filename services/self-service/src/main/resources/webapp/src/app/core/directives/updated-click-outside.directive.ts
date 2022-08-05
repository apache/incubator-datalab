import {Directive, ElementRef, Output, EventEmitter, HostListener} from '@angular/core';

@Directive({
  selector: '[datalabClickedOutside]'
})
export class UpdatedClickedOutsideDirective {
  constructor(private el: ElementRef) { }

  @Output() public clickedOutside = new EventEmitter();

  @HostListener('document:click', ['$event.target'])
  public onClick(target: any) {
    const clickedInside = this.el.nativeElement.contains(target);
    if (!clickedInside) {
      this.clickedOutside.emit(target);
    }
  }
}
