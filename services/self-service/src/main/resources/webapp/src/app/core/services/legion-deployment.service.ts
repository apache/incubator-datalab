import { Injectable } from '@angular/core';
import {from, Observable} from "rxjs";
import { map, catchError } from 'rxjs/operators';
import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable({
  providedIn: 'root'
})
export class LegionDeploymentService {
  public list =  [{clasters: [
      // {
      //   project: "Project5",
      //   endpoint: "Endpoint24",
      //   name: "claster1",
      // },
      // {
      //   project: "Project4",
      //   endpoint: "Endpoint23",
      //   name: "claster1",
      // },
      // {
      //   project: "Project3",
      //   endpoint: "Endpoint21",
      //   name: "claster1",
      // }
      ]}];
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getLegionClasters(){
    const obsList = from(this.list);
    return obsList;
  }

  public addLegionCluster(cluster) {
    this.list[0].clasters.push(cluster);
  }

  public createOduhuCluster(data): Observable<{}> {
    return this.applicationServiceFacade
      .createOdahuCluster(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getOduhuClustersList(): Observable<{}> {
    return this.applicationServiceFacade
      .getOdahuList()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
