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

import { Component, OnInit, Input, Output, EventEmitter, ViewChild, ChangeDetectorRef } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';

import { ProjectService, RolesGroupsService, EndpointService } from '../../../core/services';
import { ProjectDataService } from '../project-data.service';
import { CheckUtils, PATTERNS } from '../../../core/util';
import { Project } from '../project.component';

@Component({
  selector: 'project-form',
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.scss']
})
export class ProjectFormComponent implements OnInit {

  public projectForm: FormGroup;
  public groupsList: any = [];
  public endpointsList: any = [];
  public projectList: Project[] = [];
  public accessKeyValid: boolean;
  public keyLabel: string = '';

  @Input() item: any;
  @Output() update: EventEmitter<{}> = new EventEmitter();
  @ViewChild('stepper') stepper;

  private subscriptions: Subscription = new Subscription();

  constructor(
    public toastr: ToastrService,
    private _fb: FormBuilder,
    private projectService: ProjectService,
    private projectDataService: ProjectDataService,
    private rolesService: RolesGroupsService,
    private endpointService: EndpointService,
    private cd: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.initFormModel();
    this.getGroupsData();
    this.getEndpointsData();

    this.subscriptions.add(this.projectDataService._projects.subscribe(
      (value: Project[]) => {
        if (value) this.projectList = value;
      }));
    if (this.item) {
      this.editSpecificProject(this.item);
      this.stepper.selectedIndex = 2;
    }
  }

  public confirm(data) {
    if (this.item) {
      this.projectService.updateProject(data).subscribe(() => {
        this.toastr.success('Project updated successfully!', 'Success!');
        this.update.emit();
      }, error => this.toastr.error(error.message || 'Project update failed!', 'Oops!'));
    } else {
      this.projectService.createProject(data).subscribe(() => {
        this.toastr.success('Project creation is processing!', 'Success!');
        this.projectDataService.updateProjects();
        this.update.emit();
        this.reset();
      }, error => this.toastr.error(error.message || 'Project creation failed!', 'Oops!'));
    }
  }

  public reset() {
    this.keyLabel = '';
    this.initFormModel();
  }

  public generateProjectTag($event) {
    let user_tag = `dlab-${$event.target.value}`;
    this.projectForm.controls.tag.setValue(user_tag.toLowerCase());
  }

  public onFileChange($event) {
    const reader = new FileReader();
    const files = $event.target.files;

    if (files && files.length) {
      const [file] = $event.target.files;
      reader.readAsBinaryString(file);

      reader.onload = () => {
        this.projectForm.patchValue({
          key: reader.result
        });

        this.accessKeyValid = this.isValidKey(file.name);
        this.keyLabel = this.getLabel(file);
        $event.target.value = '';
        this.cd.markForCheck();
      };
    }
  }

  public selectOptions(list, key, select?) {
    let filter = key === 'endpoints' ? list.map(el => el.name) : list;
    this.projectForm.controls[key].setValue(select ? filter : []);
  }

  private initFormModel(): void {
    this.projectForm = this._fb.group({
      'key': ['', Validators.required],
      'name': ['', Validators.compose([Validators.required, Validators.pattern(PATTERNS.namePattern), this.checkDuplication.bind(this), this.providerMaxLength.bind(this)])],
      'endpoints': [[], Validators.required],
      'tag': ['', Validators.compose([Validators.required, Validators.pattern(PATTERNS.namePattern)])],
      'groups': [[], Validators.required]
    });
  }

  public editSpecificProject(item: Project) {

    this.projectForm = this._fb.group({
      'key': [''],
      'name': [item.name, Validators.required],
      'endpoints': [item.endpoints],
      'tag': [item.tag, Validators.required],
      'groups': [item.groups, Validators.required]
    });
  }

  private getLabel(file: File): string {
    return file ? !this.accessKeyValid ? 'Public key is required.' : file.name : '';
  }

  private isValidKey(value): boolean {
    return value.toLowerCase().endsWith('.pub');
  }

  private getGroupsData() {
    this.rolesService.getGroupsData().subscribe(
      (list: any) => this.groupsList = list.map(el => el.group),
      error => this.toastr.error(error.message, 'Oops!'));
  }

  private getEndpointsData() {
    this.endpointService.getEndpointsData().subscribe(
      list => this.endpointsList = list,
      error => this.toastr.error(error.message, 'Oops!'));
  }

  private checkDuplication(control) {
    if (control.value) {
      for (let index = 0; index < this.projectList.length; index++) {
        if (CheckUtils.delimitersFiltering(control.value) === CheckUtils.delimitersFiltering(this.projectList[index].name))
          return { duplication: true };
      }
    }
  }

  private providerMaxLength(control) {
    return control.value.length <= 50 ? null : { limit: true };
  }
}
