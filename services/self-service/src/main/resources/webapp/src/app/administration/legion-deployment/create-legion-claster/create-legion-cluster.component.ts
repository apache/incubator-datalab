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

import { Project } from '../../../administration/project/project.component';
import { UserResourceService, ProjectService, LegionDeploymentService } from '../../../core/services';

import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import {PATTERNS} from "../../../core/util";


@Component({
  selector: 'create-legion-cluster',
  templateUrl: 'create-legion-cluster.component.html',
  styleUrls: ['./create-legion-cluster.component.scss']
})

export class CreateLegionClusterComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public createLegionClusterForm: FormGroup;

  projects: Project[] = [];
  endpoints: Array<String> = [];
  minInstanceNumber = 2;
  maxInstanceNumber: number = 14;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<CreateLegionClusterComponent>,
    private _fb: FormBuilder,
    private projectService: ProjectService,
    private legionDeploymentService: LegionDeploymentService,
  ) {
  }

  ngOnInit() {
    this.getUserProjects();
    this.initFormModel();
  }

  public getProjects(): void{
    this.projectService.getProjectsList().subscribe((projects: any) => this.projects = projects);
  }

  public getUserProjects(): void {
    this.projectService.getUserProjectsList(true).subscribe((projects: any) => {
      this.projects = projects;
    });
  }

  public setEndpoints(project): void {
    this.endpoints = project.endpoints
      .filter(e => e.status === 'RUNNING')
      .map(e => e.name);
  }

  private initFormModel(): void {
    this.createLegionClusterForm = this._fb.group({
      name: ['', Validators.required],
      project: ['', Validators.required],
      endpoint: ['', Validators.required],
    });
  }

  private createOdahuCluster(value): void{
    this.dialogRef.close();
    this.legionDeploymentService.createOdahuNewCluster(value).subscribe();
  }
}
