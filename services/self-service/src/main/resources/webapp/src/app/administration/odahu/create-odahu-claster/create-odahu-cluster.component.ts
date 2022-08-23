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

import { Component, OnInit, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { ProjectService, OdahuDeploymentService } from '../../../core/services';

import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import {CheckUtils, PATTERNS} from '../../../core/util';
import { Project } from '../../project/project.model';


@Component({
  selector: 'create-odahu-cluster',
  templateUrl: 'create-odahu-cluster.component.html',
  styleUrls: ['./create-odahu-cluster.component.scss']
})

export class CreateOdahuClusterComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public createOdahuForm: FormGroup;

  projects: Project[] = [];
  endpoints: Array<String> = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<CreateOdahuClusterComponent>,
    private _fb: FormBuilder,
    private projectService: ProjectService,
    private odahuDeploymentService: OdahuDeploymentService,
  ) {
  }

  ngOnInit() {
    this.getUserProjects();
    this.initFormModel();
  }

  public getUserProjects(): void {
    this.projectService.getUserProjectsList(true).subscribe((projects: any) => {
      this.projects = projects.filter(project => {
        return project.endpoints.length > project.odahu.filter(od => od.status !== 'FAILED' && od.status !== 'TERMINATED').length; }
        );
    });
  }

  public setEndpoints(project): void {
    this.endpoints = project.endpoints
      .filter(e => e.status === 'RUNNING' && !this.data.some(odahu => odahu.status !== 'FAILED'
        && odahu.status !== 'TERMINATED'
        && odahu.endpoint === e.name
        && odahu.project === project.name)
      )
      .map(e => e.name);
  }

  private initFormModel(): void {
    this.createOdahuForm = this._fb.group({
      name: ['', [Validators.required, Validators.pattern(PATTERNS.namePattern), this.checkDuplication.bind(this)]],
      project: ['', Validators.required],
      endpoint: ['', [Validators.required]],
      custom_tag: ['', [Validators.pattern(PATTERNS.namePattern)]]
    });
  }

  public createOdahuCluster(value): void {
    this.dialogRef.close();
    this.odahuDeploymentService.createOdahuNewCluster(value).subscribe(() => {
      this.toastr.success('Odahu cluster creation is processing!', 'Success!');
      }, error => this.toastr.error(error.message || 'Odahu cluster creation failed!', 'Oops!')
    );
  }

  private checkDuplication(control) {
    if (control && control.value) {
      for (let index = 0; index < this.data.length; index++) {
        if (CheckUtils.delimitersFiltering(control.value) === CheckUtils.delimitersFiltering(this.data[index].name))
          return { duplication: true };
      }
    }
  }
}
