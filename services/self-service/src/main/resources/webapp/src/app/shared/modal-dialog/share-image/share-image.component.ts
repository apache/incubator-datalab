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
import { UserImagesPageService } from '../../../core/services';
import { ShareImageAllUsersParams } from '../../../resources/images';
import { ImagesService } from '../../../resources/images/images.service';

@Component({
  selector: 'datalab-share-image',
  templateUrl: './share-image.component.html',
  styleUrls: ['./share-image.component.scss']
})
export class ShareImageComponent implements OnInit {
  imageName!: string;

  constructor(
    public dialogRef: MatDialogRef<ShareImageComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private userImagesPageService: UserImagesPageService,
    private imagesService: ImagesService
  ) { }

  ngOnInit(): void {
    this.getImageName();
  }

  getImageName(): void {
    this.imageName = this.data.image.name;
  }

  onYesClick(): void {
    const shareParams: ShareImageAllUsersParams = {
      imageName: this.data.image.name,
      projectName: this.data.image.project,
      endpoint: this.data.image.endpoint
    };

    this.userImagesPageService.shareImageAllUsers(shareParams)
      .subscribe(projectList => this.imagesService.projectList = projectList);
    this.dialogRef.close();
  }
}
