/***************************************************************************

Copyright (c) 2017, EPAM SYSTEMS INC

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

import { Component, Input, Output, EventEmitter, HostBinding,
         ChangeDetectorRef, ElementRef, OnInit, OnDestroy,
         ViewEncapsulation } from '@angular/core';
import { BubblesCollector, BubbleService } from './bubble.service';

@Component({
  moduleId: module.id,
  selector: 'bubble-up',
  template: '<ng-content></ng-content>',
  styleUrls: ['./bubble.component.css'],
  host: {'class': 'bubble-up'},
  encapsulation: ViewEncapsulation.None
})
export class BubbleComponent implements OnDestroy {
  @Input('position') public position: string;
  @Output() onShow: EventEmitter<any> = new EventEmitter();
  @Output() onHide: EventEmitter<any> = new EventEmitter();
  @HostBinding('class.is-visible') public isVisible = false;

  constructor(
    public elementRef: ElementRef,
    private ref: ChangeDetectorRef,
    private collector: BubblesCollector,
    private bubbleService: BubbleService) {
    this.collector.addBubble(this);
  }

  public toggle(event: Event, element: any = null) {
    this.isVisible ? this.hide() : this.show(event, element);
  }

  public hide() {
    if (this.isVisible) {
      this.onHide.emit(null);
      this.isVisible = false;
      this.ref.markForCheck();
    }
  }

  private hideAllBubbles() {
    this.collector.hideAllBubbles(this);
  }

  public show(event: Event, element: any = null) {
    this.hideAllBubbles();
    event.stopPropagation();
    if (!this.isVisible) {
      this.onShow.emit(null);
      this.isVisible = true;
      this.updateDirection(event, element);
    }
  }

  private updateDirection(event: Event, element: any = null) {
    const bubbleElem = this.elementRef.nativeElement;
    bubbleElem.style.visibility = 'hidden';
    setTimeout(() => {
      if (element && this.position) {
        this.bubbleService.updatePosition(element, bubbleElem, this.position);
        bubbleElem.style.visibility = 'visible';
        this.ref.markForCheck();
        return;
      }
    });
  }

  public ngOnDestroy() {
    this.collector.removeBubble(this);
  }
}
