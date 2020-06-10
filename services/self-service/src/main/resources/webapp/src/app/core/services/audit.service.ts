import { Injectable } from '@angular/core';
import {ApplicationServiceFacade} from './applicationServiceFacade.service';
import {catchError, map} from 'rxjs/operators';
import {ErrorUtils} from '../util';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {
  }

  public getAuditData() {
    return this.applicationServiceFacade
      .getAuditList()
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
    // return [
    //   {user: 'Dlab-test-user1', action: 'Deleted users from group', project: '', date: new Date().toLocaleString(), info: {name: 'admin', objects: ['user1', 'user2', 'user3', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1']}},
    //   {user: 'Dlab-test-user1', action: 'Created project', project: 'ProjectA', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Created project', project: 'ProjectA', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Created project', project: 'ProjectA', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user2', action: 'Created notebook ', project: 'ProjectA', resource: 'Rstudio', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Deleted user to group', project: '', date: new Date().toLocaleString(), info: {name: 'admin', objects: ['user1', 'user2', 'user3', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1']}},
    //   {user: 'Dlab-test-user1', action: 'Stopped notebook', project: 'ProjectA', resource: 'Rstudio', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Started notebook', project: 'ProjectA', resource: 'Rstudio', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Deleted Users from group', project: '', date: new Date().toLocaleString(), info: {name: 'admin', objects: ['user1', 'user2', 'user3', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1']}},
    //   {user: 'Dlab-test-user3', action: 'Created EMR', project: 'ProjectA', resource: 'Rstudio:Emr1', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Created notebook', project: 'ProjectA', resource: 'Rstudio', date: new Date().toLocaleString()},
    //   {user: 'Dlab-test-user1', action: 'Deleted user to group', project: '', date: new Date().toLocaleString(), info: {name: 'admin', objects: ['user1', 'user2', 'user3', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1', 'Dlab-test-user1']}},
    //   {user: 'Dlab-test-user2', action: 'Terminated notebook', project: 'ProjectA', resource: 'Rstudio', date: new Date().toLocaleString()},
    //   ];

}
