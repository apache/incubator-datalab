import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable()

export class OdahuDeploymentService {
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

  public odahuAction(data, action) {
    return this.applicationServiceFacade
        .odahuStartStop(data, action)
        .pipe(
            map(response => response),
            catchError(ErrorUtils.handleServiceError));
  }
}
