/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { Component, OnDestroy, Input, Output, EventEmitter, ElementRef, ViewChild, ViewEncapsulation } from '@angular/core';

@Component({
  moduleId: module.id,
  selector: 'modal-dialog',
  templateUrl: 'modal.component.html',
  styleUrls: ['./modal.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class ModalComponent implements OnDestroy {

  isOpened: boolean = false;
  isHeader: boolean = true;
  isFooter: boolean = true;
  onClosing: Function;
  isHide: boolean;
  clear: number;

  @Input() modalClass: string;
  @Input() title: string;
  @Input() cancelButtonLabel: string;
  @Input() submitButtonLabel: string;
  @Input() hideCloseButton: boolean = false;
  @Input() closeOnEscape: boolean = true;

  @Output() onOpen = new EventEmitter(false);
  @Output() onClose = new EventEmitter(false);
  @Output() onSubmit = new EventEmitter(false);

  @ViewChild('modalRoot') el: ElementRef;

  private backdropElement: HTMLElement;

  constructor() {
    this.createBackDrop();
  }

  closeOnOutsideClick($event): boolean {
    return ($event.target && $event.target.id === 'modalRoot');
  }

  ngOnDestroy() {
    document.body.className = document.body.className.replace(/modal-open\b/, '');
    if (this.backdropElement && this.backdropElement.parentNode === document.body)
      document.body.removeChild(this.backdropElement);
  }

  public open(option: Object, ...args: any[]): void {
    if (this.isOpened)
      return;

    if (option) {
      for (const key in option) {
        this[key] = option[key];
      }
    }

    this.isOpened = true;
    this.isHide = false;
    this.onOpen.emit(args);
    document.body.appendChild(this.backdropElement);
    window.setTimeout(() => this.el.nativeElement.focus(), 0);
    document.body.className += ' modal-open';
  }

  public close(...args: any[]): void {
    if (!this.isOpened)
      return;

    if (this.onClosing)
      this.onClosing();
    this.onClose.emit(args);

    this.isHide = true;
    this.clear = window.setTimeout(() => {
      document.body.removeChild(this.backdropElement);
      document.body.className = document.body.className.replace(/modal-open\b/, '');
      this.el.nativeElement.classList.remove('out');
      this.isOpened = false;

      clearTimeout(this.clear);
    }, 300);

    if (document.getElementsByClassName('dropdown open').length)
      document.getElementsByClassName('dropdown open')[0].classList.remove('open');
  }

  private createBackDrop() {
    this.backdropElement = document.createElement('div');
    this.backdropElement.classList.add('backdrop');
  }
}
