/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {
  Component,
  Input,
  Output,
  EventEmitter,
  HostBinding,
  ChangeDetectorRef,
  ElementRef,
  OnDestroy,
  ViewEncapsulation,
  HostListener
} from '@angular/core';
import { BubblesCollector, BubbleService } from './bubble.service';

@Component({
  selector: 'bubble-up',
  template: '<ng-content></ng-content>',
  styleUrls: ['./bubble.component.css'],
  host: {'class': 'bubble-up'},
  encapsulation: ViewEncapsulation.None
})
export class BubbleComponent implements OnDestroy {
  public changeDirection: boolean = false;

  @Input('keep-open') public keepOpen: boolean = false;
  @Input('position') public position: string;
  @Input('alternative') public alternative: string;

  @Output() onShow: EventEmitter<any> = new EventEmitter();
  @Output() onHide: EventEmitter<any> = new EventEmitter();

  @HostBinding('class.is-visible') public isVisible = false;
  @HostListener('click', ['$event']) onClick(event) {
    this.keepOpen && event.stopPropagation();
  }

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
      this.changeDirection = false;
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

        if (this.alternative) {
          this.changeDirection = !this.isInViewport(bubbleElem);

          let isBubbleOutOfWrapper;

          if (document.querySelector('.wrapper')) {
            isBubbleOutOfWrapper = bubbleElem.getBoundingClientRect()
              .bottom > document.querySelector('.wrapper')
              .getBoundingClientRect()
              .bottom;
          }

          (this.changeDirection || isBubbleOutOfWrapper) && this.bubbleService.updatePosition(element, bubbleElem, this.alternative);
        }

        this.ref.markForCheck();
        return;
      }
    });
  }

  private isInViewport(element) {
    const rect = element.getBoundingClientRect();
    const html = document.documentElement;
    return (rect.top >= 0 && rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || html.clientHeight) &&
            rect.right <= (window.innerWidth || html.clientWidth)
    );
  }

  public ngOnDestroy() {
    this.collector.removeBubble(this);
  }
}
