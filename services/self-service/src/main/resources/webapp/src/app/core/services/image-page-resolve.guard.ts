import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router';
import { Observable, of  } from 'rxjs';

import { ProjectImagesInfo } from '../../resources/images';
import { switchMap, take } from 'rxjs/operators';
import { ImagesPageService } from './images-page.service';

@Injectable({
  providedIn: 'root'
})
export class ImagePageResolveGuard implements Resolve<ProjectImagesInfo> {
  constructor(
    private router: Router,
    private userImagesPageService: ImagesPageService
  ) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ProjectImagesInfo> {
    return this.userImagesPageService.getFilterImagePage().pipe(
      switchMap((imagePageData: ProjectImagesInfo) => of(imagePageData)),
      take(1)
    );
  }
}
