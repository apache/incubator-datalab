import { Injectable } from '@angular/core';
import { ImageModel, ProjectModel, ShareImageAllUsersParams } from './images.model';
import { Observable } from 'rxjs';
import { UserImagesPageService } from '../../core/services';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ImagesService {

  projectList: ProjectModel[];

  constructor(
    private userImagesPageService: UserImagesPageService
  ) { }

  shareImageAllUsers(image: ImageModel): Observable<ProjectModel[]> {
    const shareParams: ShareImageAllUsersParams = {
      imageName: image.name,
      projectName: image.project,
      endpoint: image.endpoint
    };
    return this.userImagesPageService.shareImageAllUsers(shareParams).pipe(
      tap((response: ProjectModel[]) => this.projectList = response)
    );
  }
}
