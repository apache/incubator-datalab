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
