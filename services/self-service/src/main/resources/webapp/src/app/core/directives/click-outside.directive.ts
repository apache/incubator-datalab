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


import { fromEvent as observableFromEvent } from 'rxjs';

import { tap, delay } from 'rxjs/operators';
import { Directive, OnInit, OnDestroy, Output, EventEmitter, ElementRef } from '@angular/core';

@Directive({
  selector: '[clickOutside]'
})

export class ClickOutsideDirective implements OnInit, OnDestroy {

  @Output() clickOutside: EventEmitter<Object>;

  private listening: boolean;
  private globalClick: any;

  constructor(private _elRef: ElementRef) {
    this.listening = false;
    this.clickOutside = new EventEmitter();
  }

  ngOnInit() {
    this.globalClick = observableFromEvent(document, 'click').pipe(
      delay(1),
      tap(() => {
        this.listening = true;
      })).subscribe(($event: MouseEvent) => {
        this.onGlobalClick($event);
      });
  }

  ngOnDestroy() {
    this.globalClick.unsubscribe();
  }

  onGlobalClick($event: MouseEvent) {
    if ($event instanceof MouseEvent && this.listening === true) {
      if (this.isDescendant(this._elRef.nativeElement, $event.target) === true) {
        this.clickOutside.emit({
          target: ($event.target || null),
          value: false
        });
      } else {
        this.clickOutside.emit({
          target: ($event.target || null),
          value: true
        });
      }
    }
  }

  isDescendant(parent, child) {
    let node = child;
    while (node !== null) {
      if (node === parent) { return true; } else { node = node.parentNode; }
    }
    return false;
  }
}
