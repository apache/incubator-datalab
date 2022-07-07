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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ImagesService } from '../../../resources/images/images.service';
import { UserImagesPageService } from '../../../core/services';
import { Toaster_Message } from '../../../resources/images';
import { ToastrService } from 'ngx-toastr';
import { tap } from 'rxjs/operators';

@Component({
  selector: 'datalab-share-image',
  templateUrl: './share-image.component.html',
  styleUrls: ['./share-image.component.scss']
})
export class ShareImageComponent {
  imageName!: string;

  constructor(
    public dialogRef: MatDialogRef<ShareImageComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private imagesService: ImagesService,
    private userImagesPageService: UserImagesPageService,
    private toastr: ToastrService,
  ) { }

  onShare() {
    this.dialogRef.close();
    this.imagesService.shareImageAllUsers(this.data.image).pipe(
      tap(response => this.imagesService.projectList = response)
    ).subscribe(
      () => this.toastr.success(Toaster_Message.successShare, 'Success!')
    );
  }
}
