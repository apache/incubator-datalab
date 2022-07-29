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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AbstractControl, FormBuilder, FormControl, FormGroup} from '@angular/forms';

import {FilterFormPlaceholders} from './page-filter.config';
import {DropdownFieldNames, FilterDropdownValue} from '../../images';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';

@Component({
  selector: 'datalab-page-filter',
  templateUrl: './page-filter.component.html',
  styleUrls: ['./page-filter.component.scss']
})
export class PageFilterComponent implements OnInit {
  @Input() $filterDropdownData: Observable<FilterDropdownValue>;

  @Output() filterFormValue: EventEmitter<FilterDropdownValue> = new EventEmitter<FilterDropdownValue>();
  @Output() closeFilter: EventEmitter<any> = new EventEmitter<any>();
  @Output() imageNameValue: EventEmitter<string> = new EventEmitter<string>();
  @Output() onValueChanges: EventEmitter<string> = new EventEmitter<string>();

  readonly placeholders: typeof FilterFormPlaceholders = FilterFormPlaceholders;

  filterForm: FormGroup;

  constructor(
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {
    this.initFilterForm();
    this.onControlChange(DropdownFieldNames.imageName);
  }

  initFilterForm(): void {
    this.filterForm = this.fb.group({
      imageName: '',
      cloudProviders: [[]],
      statuses: [[]],
      templateNames: [[]]
    });
  }

  onSelectClick(): void {
    const dropdownList = document.querySelectorAll('.cdk-overlay-pane');
    dropdownList.forEach(el => el.classList.add('normalized-dropdown-position'));
  }

  confirmFilter(): void {
    this.filterFormValue.emit(this.filterForm.value);
  }

  cancelFilter(): void {
    this.closeFilter.emit();
  }

  onControlChange(fieldName: keyof FilterDropdownValue): void {
      console.log(this.filterForm.get(fieldName));
   this.filterForm.get(fieldName)?.valueChanges.pipe(
      tap((inputValue: string) => this.onValueChanges.emit(inputValue))
    ).subscribe();

  }
}
