/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import {ConfirmationDialogType} from "./confirmation-dialog-type.enum";
import {Observable} from "rxjs";
import {Response} from "@angular/http";
import {UserResourceService} from "../../services/userResource.service";

export class ConfirmationDialogModel {
  private title : string;
  private notebook: any;
  private confirmAction : Function;
  private userResourceService : UserResourceService;
  constructor(confirmationType : ConfirmationDialogType,
              notebook : any,
              fnProcessResults: any,
              fnProcessErrors: any,
              userResourceService : UserResourceService
              ) {
    this.userResourceService = userResourceService;
    this.setup(confirmationType, notebook, fnProcessResults, fnProcessErrors);
  }

  static getDefault () : ConfirmationDialogModel
  {
    return new
      ConfirmationDialogModel(
        ConfirmationDialogType.StopExploratory, {name: "", resources: []}, () => {},  () => {}, null);
  }

  private stopExploratory() : Observable<Response> {
    return this.userResourceService.suspendExploratoryEnvironment(this.notebook, "stop");
  }

  private terminateExploratory() : Observable<Response> {
    return this.userResourceService.suspendExploratoryEnvironment(this.notebook, "terminate")
  }

  private setup(confirmationType : ConfirmationDialogType, notebook : any, fnProcessResults : any, fnProcessErrors: any) : void
  {
    switch (confirmationType)
    {
      case ConfirmationDialogType.StopExploratory: {
        this.title = "Exploratory Environment will be stopped and all connected computational resources will be terminated.";
        this.notebook = notebook;
        this.confirmAction = () => this.stopExploratory()
          .subscribe((response : Response) => fnProcessResults(response),
            (response: Response) => fnProcessErrors(response));
      }
      break;
      case ConfirmationDialogType.TerminateExploratory: {
        this.title = "Exploratory Environment and all connected computational resources will be terminated.";
        this.notebook = notebook;
        this.confirmAction = () => this.terminateExploratory()
          .subscribe((response : Response) => fnProcessResults(response),
            (response: Response) => fnProcessErrors(response));
      }
      break;
      default: {
        this.title = "Exploratory Environment and all connected computational resources will be terminated.";
        this.notebook = notebook;
        this.confirmAction = () => this.stopExploratory()
          .subscribe((response : Response) => fnProcessResults(response),
            (response: Response) => fnProcessErrors(response));
      }
      break;
    }
  }
}
