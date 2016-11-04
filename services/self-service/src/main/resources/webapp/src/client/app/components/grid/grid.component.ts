/******************************************************************************************************

Copyright (c) 2016 EPAM Systems Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*****************************************************************************************************/

import { Component, EventEmitter, Input, Output, ViewChild, OnInit } from "@angular/core";
import { UserResourceService } from "./../../services/userResource.service";
import { GridRowModel } from './grid.model';
import { CreateEmrModel } from "./createEmrModel";

@Component({
  moduleId: module.id,
  selector: 'ng-grid',
  templateUrl: 'grid.component.html',
  styleUrls: ['./grid.component.css']
})

export class Grid implements OnInit {

  isFilled: boolean = false;
  list: any;
  environments: Array<GridRowModel>;
  notebookName: any;

  model = new CreateEmrModel('', '');
  namePattern = "/S+";

  @ViewChild('createEmrModal') createEmrModal;
  @ViewChild('confirmationDialog') confirmationDialog;
  @ViewChild('detailDialog') detailDialog;
  @Input() emrTempls;
  @Input() shapes;


  constructor(
    private userResourceService: UserResourceService
    ) { }

  ngOnInit() {
    this.buildGrid();
  }

  buildGrid() {
    this.userResourceService.getUserProvisionedResources().subscribe((list) => {
      this.list = list;
      this.environments = this.loadEnvironments();
      console.log('models ', this.environments);
    });
  }

  containsNotebook(notebook_name):boolean {

    if(notebook_name)
      for (var index = 0; index < this.environments.length; index++)
        if(notebook_name.toLowerCase() ==  this.environments[index].name.toString().toLowerCase())
          return true;

        return false;
  }

  loadEnvironments(): Array<any> {
     if (this.list) {
       return this.list.map((value) => {
         return new GridRowModel(value.exploratory_name,
           value.status,
           value.shape,
           value.computational_resources);
       });
     }
   }

  printDetailEnvironmentModal(data) {
    this.detailDialog.open({ isFooter: false }, data);
  }

  mathAction(data, action) {
    console.log('action ' + action, data);
    if (action === 'deploy') {
      this.notebookName = data.name
      this.createEmrModal.open({ isFooter: false });
    } else if (action === 'run') {
      this.userResourceService
        .runExploratoryEnvironment({notebook_instance_name: data.name})
        .subscribe((result) => {
          console.log('startUsernotebook result: ', result);
          this.buildGrid();
        });
    } else if (action === 'stop') {
      this.confirmationDialog.open({ isFooter: false }, data, 'stop');
    } else if (action === 'terminate') {
      this.confirmationDialog.open({ isFooter: false }, data, 'terminate');
    }
  }

  createEmr(name, count, shape_master, shape_slave, tmplIndex){

    this.userResourceService
      .createComputationalResource({
        name: name,
        emr_instance_count: count,
        emr_master_instance_type: shape_master,
        emr_slave_instance_type: shape_slave,
        emr_version: this.emrTempls[tmplIndex].version,
        notebook_name: this.notebookName
      })
      .subscribe((result) => {
        console.log('result: ', result);

        if (this.createEmrModal.isOpened) {
         this.createEmrModal.close();
       }
       this.buildGrid();
      });
      return false;
  };

  validate(name, count) {
    this.isFilled = (name.value.length > 1) && (+count.value > 0);
  }
}
