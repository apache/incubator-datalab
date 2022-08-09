import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router';
import { Observable, of  } from 'rxjs';

import { ImagesService } from '../../resources/images/images.service';
import { ProjectImagesInfo } from '../../resources/images';
import { switchMap, take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ImagePageResolveGuard implements Resolve<ProjectImagesInfo> {
  constructor(
    private router: Router,
    private imagesService: ImagesService
  ) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ProjectImagesInfo> {
    return this.imagesService.getImagePageInfo().pipe(
      switchMap((imagePageData: ProjectImagesInfo) => of(imagePageData)),
      take(1)
    );
  }
}
