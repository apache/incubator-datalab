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

import { Directive, ElementRef, HostListener, Input, OnInit, Renderer2 } from '@angular/core';

import { ModifiedEndpoint } from '../../administration/project/project.model';
import { checkEndpointListUtil } from '../util';


@Directive({
  selector: '[datalabIsEndpointActive]'
})
export class IsEndpointsActiveDirective implements OnInit {
  @Input() endpointList: ModifiedEndpoint[];

  private isButtonDisabled: boolean = false;

  constructor (
    private el: ElementRef,
    private renderer: Renderer2
  ) { }

  ngOnInit(): void {
    this.checkEndpointList(this.endpointList);
    this.setStyle();
  }

  @HostListener('click', ['$event'])
  onClick(event: Event): void {
    if (this.isButtonDisabled) {
      event.stopPropagation();
    }
  }

  private setStyle() {
    if (this.isButtonDisabled) {
      this.renderer.addClass(this.el.nativeElement, 'disabled-button');
    }
  }

  private checkEndpointList(endpointList: ModifiedEndpoint[]): void {
    this.isButtonDisabled = checkEndpointListUtil(endpointList);
  }
}
