import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable} from "rxjs";
import {LegionDeploymentService} from "../../core/services";


@Injectable({
  providedIn: 'root'
})

export class LegionDeploymentDataService {
  _legionClasters = new BehaviorSubject<any>(null);

  constructor(private legionDeploymentService: LegionDeploymentService) {
    this.getClastersList();
  }

  public updateClasters(): void {
    this.getClastersList();
  }

  private getClastersList(): void {
   this.legionDeploymentService.getLegionClasters().subscribe(
      (response: any ) => this._legionClasters.next(response.clasters));
  }
}
