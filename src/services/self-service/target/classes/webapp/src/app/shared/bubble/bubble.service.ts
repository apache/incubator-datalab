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

import { Injectable, Inject, } from '@angular/core';
import { DOCUMENT } from '@angular/common';

import { BubbleComponent } from './bubble.component';

@Injectable()
export class BubbleService {
  constructor() { }

  public updatePosition(element: HTMLElement, bubbleElement: HTMLElement, position: string): void {
    const coordinates = this.calculateCoordinates(element, bubbleElement, position);
    if (coordinates) {
      bubbleElement.style.left = coordinates.left + 'px';
      bubbleElement.style.top = coordinates.top + 'px';
    }
  }

  private calculateCoordinates(element: HTMLElement, bubbleElement: HTMLElement, position: string) {
    if (!element || !position) return null;

    const positionMap = {
      'bottom-left': {
        top: element.offsetTop + element.offsetHeight * 2,
        left: element.offsetLeft + element.offsetWidth - bubbleElement.offsetWidth
      },
      'bottom-right': { top: element.offsetTop + element.offsetHeight, left: element.offsetLeft },
      'top-left': {
        top: element.offsetTop - bubbleElement.offsetHeight,
        left: element.offsetLeft + element.offsetWidth - bubbleElement.offsetWidth
      },
      'top-right': { top: element.offsetTop - bubbleElement.offsetHeight, left: element.offsetLeft }
    };

    return positionMap[position];
  }
}

@Injectable()
export class BubblesCollector {
  private bubbles: Array<BubbleComponent> = [];

  constructor(@Inject(DOCUMENT) private document: any) {
    this.document.addEventListener('click', () => {
      this.bubbles.filter((component: BubbleComponent) => component.isVisible)
        .forEach((component: BubbleComponent) => component.hide());
    });
  }

  public addBubble(bubbleComponent: BubbleComponent) {
    this.bubbles.push(bubbleComponent);
  }

  public removeBubble(bubble: BubbleComponent) {
    this.bubbles.slice(this.bubbles.indexOf(bubble), 1);
  }

  public hideAllBubbles(bubbleComponent: BubbleComponent) {
    this.bubbles.forEach((component) => {
      if (component !== bubbleComponent)
        component.hide();
    });
  }
}
