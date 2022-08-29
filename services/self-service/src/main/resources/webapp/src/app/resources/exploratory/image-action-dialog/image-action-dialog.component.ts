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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ImagesService } from '../../images/images.service';
import { ImageActions, ImageActionType, ImageModalData, ImageParams, Toaster_Message } from '../../images';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'datalab-image-action-dialog',
  templateUrl: './image-action-dialog.component.html',
  styleUrls: ['./image-action-dialog.component.scss']
})
export class ImageActionDialogComponent implements OnInit {
  readonly actionType: typeof ImageActions = ImageActions;

  imageName!: string;
  title!: string;

  constructor(
    public dialogRef: MatDialogRef<ImageActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ImageModalData,
    private imagesService: ImagesService,
    private toastr: ToastrService,
  ) { }

  ngOnInit(): void {
    this.imageName = this.data.image.name;
    this.setTitle();
  }

  onYesClick(actionType: ImageActionType): void {
    const actionConfig = this.getActionConfig();
    const actionHandler = actionConfig[actionType].bind(this);
    return actionHandler();
  }

  private getActionConfig() {
    return {
      share: this.shareImage,
      terminate: this.terminateImage
    };
  }

  private terminateImage() {
    const imageInfo = this.getImageInfo();
    this.dialogRef.close();
    this.imagesService.terminateImage(imageInfo, this.data.actionType)
      .subscribe();
  }

  private shareImage(): void {
    const imageInfo = this.getImageInfo();
    this.dialogRef.close();
    this.imagesService.shareImageAllUsers(imageInfo)
      .subscribe(
        () => this.toastr.success(Toaster_Message.successShare, 'Success!')
      );
  }

  private setTitle(): void {
    this.title = this.data.actionType === ImageActions.share ? 'Share Image' : 'Terminate image';
  }

  private getImageInfo(): ImageParams {
    const { name, project, endpoint } = this.data.image;
    return {
      imageName: name,
      projectName: project,
      endpoint: endpoint
    };
  }
}
