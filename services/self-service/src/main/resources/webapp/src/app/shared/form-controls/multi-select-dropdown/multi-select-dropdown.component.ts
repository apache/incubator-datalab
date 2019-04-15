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

import { Input, Output, Component, EventEmitter } from '@angular/core';

@Component({
  selector: 'multi-select-dropdown',
  templateUrl: 'multi-select-dropdown.component.html',
  styleUrls: ['../dropdowns.component.scss']
})

export class MultiSelectDropdownComponent {
  isOpen: boolean = false;

  @Input() items: Array<any>;
  @Input() model: Array<any>;
  @Input() type: string;
  @Output() selectionChange: EventEmitter<{}> = new EventEmitter();

  toggleSelectedOptions($event, model, value) {
    const index = model.indexOf(value);
    (index >= 0) ? model.splice(index, 1) : model.push(value);

    this.onUpdate($event);
    $event.preventDefault();
  }

  selectAllOptions($event) {
    this.model = [];
    this.items.forEach((item) => { this.model.push(item); });

    this.onUpdate($event);
    $event.preventDefault();
  }

  deselectAllOptions($event) {
    this.model = [];
    this.onUpdate($event);
    $event.preventDefault();
  }

  onUpdate($event): void {
    this.selectionChange.emit({ model: this.model, type: this.type, $event });
  }
}
