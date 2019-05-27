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

 import { Component, OnInit, Input } from '@angular/core';
 import { FormGroup, FormBuilder, Validators } from '@angular/forms';
 import { ToastrService } from 'ngx-toastr';

 import { ProjectService,RolesGroupsService, EndpointService } from '../../../core/services';
 import { ProjectDataService } from '../project-data.service';
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

  @Input() item: any;

  constructor(
    public toastr: ToastrService,
    private _fb: FormBuilder,
    private projectService: ProjectService,
    private projectDataService: ProjectDataService,
    private rolesService: RolesGroupsService,
    private endpointService: EndpointService
  ) { }

  ngOnInit() {
    this.initFormModel();
    this.getGroupsData();
    this.getEndpointsData();

    this.item && this.editSpecificProject(this.item);
  }

  public createProject(data) {
    console.log(data);

    this.projectService.createProject(data).subscribe(response => {
      response && this.toastr.success('Project created successfully!', 'Success!');
      this.projectDataService.updateProjects();
      this.reset();
    }, error => this.toastr.error(error.message || 'Project creation failed!', 'Oops!'));
  }

  public reset() {
    this.initFormModel();
  }

  public generateProjectTag($event) {
    let user_tag = `dlab-${ $event.target.value }`;
    this.projectForm.controls.project_tag.setValue(user_tag.toLowerCase());
  }

  public selectOptions(list, key, select?) {
    debugger;
    this.projectForm.controls[key].setValue(select ? list : []);
  }

  private initFormModel(): void {
    this.projectForm = this._fb.group({
      'project_name': ['', Validators.required],
      'endpoints_list': [[], Validators.required],
      'project_tag': ['', Validators.required],
      'users_group': [[], Validators.required]
    });
  }

  public editSpecificProject(item: Project) {

    this.projectForm = this._fb.group({
      'project_name': [item.project_name, Validators.required],
      'endpoints_list': [item.endpoints_list,Validators.required],
      'project_tag': [item.project_tag, Validators.required],
      'users_list': [item.users_list, Validators.required]
    });
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
}
