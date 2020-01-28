import { Injectable } from '@angular/core';
import {from, Observable} from "rxjs";
import { map, catchError } from 'rxjs/operators';
import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable()

export class LegionDeploymentService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public createOdahuNewCluster(data): Observable<{}> {
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
