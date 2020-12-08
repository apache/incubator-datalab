import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {OdahuDeploymentService} from '../../core/services';


@Injectable({
  providedIn: 'root'
})

export class OdahuDataService {
  _odahuClasters = new BehaviorSubject<any>(null);

  constructor(private odahuDeploymentService: OdahuDeploymentService) {
    this.getClastersList();
  }

  public updateClasters(): void {
    this.getClastersList();
  }

  private getClastersList(): void {
   this.odahuDeploymentService.getOduhuClustersList().subscribe(
      (response: any ) => {
        return this._odahuClasters.next(response);
      });
  }
}
