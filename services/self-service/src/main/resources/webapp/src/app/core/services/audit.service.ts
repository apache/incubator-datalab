import { Injectable } from '@angular/core';
import {ApplicationServiceFacade} from './applicationServiceFacade.service';
import {catchError, map} from 'rxjs/operators';
import {ErrorUtils} from '../util';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getAuditData(filterData, page, itemsPrPage) {
    let queryString = `?page-number=${page}&page-size=${itemsPrPage}`;
    if (filterData.project.length) {
      queryString += `&projects=${filterData.project.join(',')}`;
    }
    if (filterData.resource.length) {
      queryString += `&resource-names=${filterData.resource.join(',')}`;
    }
    if (filterData.users.length) {
      queryString += `&users=${filterData.users.join(',')}`;
    }
    if (filterData.date_start) {
      queryString += `&date-start=${filterData.date_start}`;
    }
    if (filterData.date_end) {
      queryString += `&date-end=${filterData.date_end}`;
    }

    return this.applicationServiceFacade
      .getAuditList(queryString)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public sendDataToAudit(data) {
    return this.applicationServiceFacade
      .postActionToAudit(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
