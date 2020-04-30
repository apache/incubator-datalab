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

import {Component, OnInit} from '@angular/core';
import {FilterAuditModel} from '../filter-audit.model';

@Component({
  selector: 'dlab-audit-grid',
  templateUrl: './audit-grid.component.html',
  styleUrls: ['./audit-grid.component.scss'],

})
export class AuditGridComponent implements OnInit {
  public auditData: Array<object>;
  public displayedColumns: string[] = ['user', 'project', 'resource', 'action', 'date'];
  public displayedFilterColumns: string[] = ['user-filter', 'project-filter', 'resource-filter', 'action-filter', 'date-filter'];
  public collapseFilterRow: boolean = true;
  public filterConfiguration: FilterAuditModel = new FilterAuditModel([], [], [], [],'', '');
  public filterAuditData: FilterAuditModel = new FilterAuditModel([], [], [], [],'', '');

  ngOnInit() {}

  public refreshAudit(auditData) {
    this.auditData = auditData;
  }

  toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  onUpdate($event): void {
    this.filterAuditData[$event.type] = $event.model;
  }
}
