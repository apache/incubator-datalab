import { Injectable } from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot} from '@angular/router';
import {EMPTY, Observable, of} from 'rxjs';

import {ImagesService} from '../../resources/images/images.service';
import {ProjectModel} from '../../resources/images';
import {switchMap, take} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ImagePageResolveGuard implements Resolve<ProjectModel[]> {
  constructor(
    private router: Router,
    private imagesService: ImagesService
  ) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ProjectModel[]> {
    return this.imagesService.getUserImagePageInfo().pipe(
      switchMap((projectList: ProjectModel[]) => of(projectList)),
      take(1)
    );
  }
}
