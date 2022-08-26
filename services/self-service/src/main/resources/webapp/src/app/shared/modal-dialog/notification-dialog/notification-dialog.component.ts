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

import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { Endpoint } from '../../../administration/project/project.model';

@Component({
  selector: 'notification-dialog',
  template: `
      <div id="dialog-box">
          <header class="dialog-header">
              <h4 class="modal-title"><i class="material-icons">priority_high</i>Warning</h4>
              <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
          </header>
          <div mat-dialog-content class="content message scrolling">
            <div *ngIf="data.type === 'terminateNode'" class="table-header">
              <div *ngIf="data.item.action.endpoint.length > 0">
                Edge node<span *ngIf="data.item.action.endpoint.length>1">s</span>&nbsp;<span class="strong">{{ data.item.action.endpoint.join(', ') }}</span> in project
                <span class="strong">{{ data.item.action.project_name }}</span> will be terminated.
              </div>
            </div>
              <div  *ngIf="data.type === 'list'" class="info pb-10">
                  <div class="quota-message" *ngIf="data.template.notebook?.length > 0">
                      Following notebook server<span *ngIf="data.template.notebook.length>1 || data.template.notebook[0].notebook.length>1">s</span><span *ngFor="let item of data.template.notebook">
                        <span class="strong blue" *ngFor="let notebook of item.notebook; let i = index">{{i === 0 ? '' : ', '}} {{ notebook }}</span> in project <span
                        class="strong blue">{{ item.project }}</span>
                        <span *ngIf="data.template.notebook.length > 1">, </span>
                      </span> will be stopped and all computes will be stopped/terminated
                  </div>

                  <div class="quota-message" *ngIf="data.template.cluster?.length > 0">
                      <p>
                          Computational resource<span *ngIf="data.template.cluster.length > 1">s</span>&nbsp;<span *ngFor="let item of data.template.cluster; let i = index">{{i === 0 ? '' : ', '}}<span class="strong blue">{{ item.computational_name }}</span> for <span
                                class="strong blue">{{ item.exploratory_name }}</span> in project <span
                          class="strong blue">{{ item.project }}</span>
                          </span>
                          will be stopped
                      </p>
                  </div>
                  <span class="strong blue pb-10">by a schedule in less than 15 minutes</span>.
              </div>
              <div class="alert" *ngIf="data.type === 'message'">
                <span  class='highlight'[innerHTML]="data.template"></span>
              </div>
              <div *ngIf="data.type === 'confirmation'" class="confirm-dialog">
                  <p *ngIf="data.template; else label">
                      <span [innerHTML]="data.template"></span>
                  </p>
                  <ng-template #label>
                      <p>
            <span *ngIf="!!data.list">Endpoint</span>
            <span *ngIf="data.action && data.action === 'decommissioned'">Project</span>
            <span class="ellipsis strong" matTooltip="{{ data.item.name }}" matTooltipPosition="above"
                  [matTooltipDisabled]="data.item.name.length > 35">
             {{ data.item.name }}</span> will be {{ data.action || 'disconnected' }}.
                      </p>
                  </ng-template>

                  <div *ngIf="data.list && data.list.length && data.type === 'confirmation'">
                      <div class="resource-list">
                          <div class="resource-list-header">
                              <div class="resource-name">Resource</div>
                              <div class="project">Project</div>
                          </div>
                          <div class="scrolling-content resource-heigth scrolling">
                              <div class="resource-list-row sans node" *ngFor="let project of data.list">
                                  <div class="resource-name ellipsis">
                                      <div>Edge node</div>
                                      <div *ngFor="let notebook of project['resource']">{{notebook['exploratory_name']}}</div>
                                  </div>
                                  <div class="project ellipsis">{{project.name}}</div>
                              </div>
                          </div>
                      </div>
<!--                      <div class="confirm-resource-terminating">-->
<!--                          <label>-->
<!--                              <input class="checkbox" type="checkbox"-->
<!--                                     (change)="terminateResource()"/>Do not terminate all related resources-->
<!--                          </label>-->
<!--                      </div>-->
                      <p class="confirm-message">
                          <span *ngIf="!willNotTerminate">All connected computes will be terminated as well.</span>
                      </p>
                  </div>
                  <mat-list *ngIf="data.item.endpoints?.length">
                      <mat-list-item class="list-header sans">
                          <div class="endpoint">Edge node in endpoint</div>
                          <div class="status">Further status</div>
                      </mat-list-item>
                      <div class="scrolling-content scrolling">
                          <mat-list-item *ngFor="let endpoint of filterEndpoints()" class="sans node">
                              <div class="endpoint ellipsis">{{endpoint.name}}</div>
                              <div class="status terminated">Terminated</div>
                          </mat-list-item>
                      </div>
                  </mat-list>
                <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
                  <div class="text-center m-top-30 m-bott-10">
                      <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
                      <button type="button" class="butt butt-success" mat-raised-button
                              (click)="dialogRef.close(true)">Yes
                      </button>
                  </div>
              </div>
               <div class="confirm-dialog" *ngIf="data.type === 'terminateNode'">
                 <mat-list *ngIf="data.item.resources.length > 0; else noResources">
                   <mat-list-item class="list-header sans">
                     <div class="endpoint">Resources</div>
                     <div class="status">Further status</div>
                   </mat-list-item>
                   <div class="scrolling-content scrolling">
                     <mat-list-item *ngFor="let resource of data.item.resources" class="sans node">
                       <div class="endpoint ellipsis">{{resource}}</div>
                       <div class="status terminated">Terminated</div>
                     </mat-list-item>
                   </div>
                 </mat-list>
                 <ng-template #noResources>
                   There are not related resources to this edge node.
                 </ng-template>
                   <div mat-dialog-content class="bottom-message" *ngIf="data.item.resources.length > 0">
                     <span class="confirm-message">All connected computes will be terminated as well.</span>
                   </div>
                 <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
                 <div class="text-center m-top-30 m-bott-10">
                   <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
                   <button type="button" class="butt butt-success" mat-raised-button
                           (click)="dialogRef.close(true)">Yes
                   </button>
                 </div>
               </div>
          </div>
      </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; max-height: 75vh; }
    .info { color: #35afd5; }
    .info .confirm-dialog { color: #607D8B; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    .plur { font-style: normal; }
    .scrolling-content{overflow-y: auto; max-height: 200px; border-bottom: 1px solid #edf1f5; }
    .endpoint { width: 70%; text-align: left; color: #577289;}
    .status { width: 30%;text-align: left;}
    .label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif;}
    .node { font-weight: 300;}
    .resource-name { width: 280px;text-align: left; padding: 10px 0;line-height: 26px;}
    .project { width: 30%;text-align: left; padding: 10px 0;line-height: 26px;}
    .resource-list{max-width: 100%; margin: 0 auto;margin-top: 20px; }
    .resource-list-header{display: flex; font-weight: 600; font-size: 14px;height: 48px; border-top: 1px solid #edf1f5; border-bottom: 1px solid #edf1f5; padding: 0 20px;}
    .resource-list-row{display: flex; border-bottom: 1px solid #edf1f5;padding: 0 20px;}
    .resource-list-row:last-child{border-bottom: none}
    .confirm-resource-terminating{text-align: left; padding: 10px 20px;}
    .confirm-message{color: #ef5c4b;font-size: 13px;min-height: 18px; text-align: center; padding-top: 20px}
    .checkbox{margin-right: 5px;vertical-align: middle; margin-bottom: 3px;}
    label{cursor: pointer}
    .bottom-message{padding-top: 15px; overflow: hidden}
    .table-header{padding-bottom: 10px;}
    .alert{text-align: left; line-height: 22px; padding-bottom: 25px;padding-top: 15px;}
    .quota-message{padding-top: 10px}
    .mat-list-base .mat-list-item { font-size: 15px}
  `]
})
export class NotificationDialogComponent {
  public willNotTerminate: boolean = false;
  public notFailedEndpoints: Endpoint [];
  constructor(
    public dialogRef: MatDialogRef<NotificationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    if (this.data.list) {
    this.willNotTerminate = !this.data.list.length;
    }
  }

  public terminateResource(): void {
    this.willNotTerminate = !this.willNotTerminate;
  }

  public filterEndpoints() {
    return this.data.item.endpoints.filter(e => e.status !== 'FAILED' && e.status !== 'TERMINATED');
  }
}
