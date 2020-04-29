import { Injectable } from '@angular/core';
import {ApplicationServiceFacade} from './applicationServiceFacade.service';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {
  }

  public getAuditData() {
    return [
      {user: 'Dlab-test-user1', action: 'Created project ProjectA', date: new Date().toLocaleString()},
      {user: 'Dlab-test-user2', action: 'Created notebook Rstudio', date: new Date().toLocaleString()},
      {user: 'Dlab-test-user1', action: 'Stopped notebook Rstudio', date: new Date().toLocaleString()},
      {user: 'Dlab-test-user1', action: 'Started notebook Rstudio', date: new Date().toLocaleString()},
      {user: 'Dlab-test-user3', action: 'Created EMR Emr1', date: new Date().toLocaleString()},
      {user: 'Dlab-test-user1', action: 'Created notebook Rstudio2', date: new Date().toLocaleString()},
      {user: 'Dlab-test-user2', action: 'Terminated notebook Rstudio', date: new Date().toLocaleString()},
      ];
  }

}
