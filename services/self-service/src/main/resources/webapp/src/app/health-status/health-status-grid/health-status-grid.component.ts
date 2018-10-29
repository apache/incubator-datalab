/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { Component, OnInit, ViewChild, Input, Output, EventEmitter, ViewContainerRef } from '@angular/core';
import { ToastsManager } from 'ng2-toastr';

import { EnvironmentStatusModel } from '../environment-status.model';
import { HealthStatusService, UserAccessKeyService } from '../../core/services';
import { ConfirmationDialogType } from '../../shared';
import { FileUtils } from '../../core/util';

@Component({
  selector: 'health-status-grid',
  templateUrl: 'health-status-grid.component.html',
  styleUrls: ['./health-status-grid.component.css',
              '../../resources/resources-grid/resources-grid.component.css']
})
export class HealthStatusGridComponent implements OnInit {

   @Input() environmentsHealthStatuses: Array<EnvironmentStatusModel>;
   @Input() anyEnvInProgress: boolean;
   @Input() notebookInProgress: boolean;
   @Input() uploadKey: boolean;
   @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();

   @ViewChild('confirmationDialog') confirmationDialog;
   @ViewChild('keyReuploadDialog') keyReuploadDialog;

    constructor(
      private healthStatusService: HealthStatusService,
      private userAccessKeyService: UserAccessKeyService,
      public toastr: ToastsManager,
      public vcr: ViewContainerRef
    ) {
      this.toastr.setRootViewContainerRef(vcr);
    }

    ngOnInit(): void { }
    
    buildGrid(): void {
      this.refreshGrid.emit();
    }

    healthStatusAction(data, action: string) {
      if (action === 'run') {
        this.healthStatusService
          .runEdgeNode()
          .subscribe(() => {
            this.buildGrid();
            // this.toastr.success('Edge node is starting!', 'Processing!', { toastLife: 5000 });
          }, error => this.toastr.error('Edge Node running failed!', 'Oops!', { toastLife: 5000 }));
      } else if (action === 'stop') {
        this.confirmationDialog.open({ isFooter: false }, data, ConfirmationDialogType.StopEdgeNode);
      } else if (action === 'recreate') {
        this.healthStatusService
          .recreateEdgeNode()
          .subscribe(() => {
            this.buildGrid();
            // this.toastr.success('Edge Node recreation is processing!', 'Processing!', { toastLife: 5000 });
          }, error => this.toastr.error('Edge Node recreation failed!', 'Oops!', { toastLife: 5000 }));
      }
    }

    showReuploaKeydDialog() {
      this.keyReuploadDialog.open({ isFooter: false });
    }

    public generateUserKey($event) {
      this.userAccessKeyService.regenerateAccessKey().subscribe(data => {
        FileUtils.downloadFile(data);
        this.buildGrid();
      });
    }
}
