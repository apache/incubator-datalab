/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

import { Injectable } from '@angular/core';
import {Response} from '@angular/http';
import {ApplicationServiceFacade} from "./applicationServiceFacade.service";

@Injectable()
export class UserResourceService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {
  }

  getExploratoryEnvironmentTemplates()
  {
    return this.applicationServiceFacade
      .buildGetExploratoryEnvironmentTemplatesRequest()
      .map(( res:Response ) => res.json())
      .catch((error: any) => error);
  }

  getComputationalResourcesTemplates()
  {
    return this.applicationServiceFacade
      .buildGetComputationalResourcesTemplatesRequest()
      .map(( res:Response ) => res.json())
      .catch((error: any) => error);
  }

  getSupportedResourcesShapes()
  {
    return this.applicationServiceFacade
      .buildGetSupportedComputationalResourcesShapesRequest()
      .map(( res:Response ) => res.json())
      .catch((error: any) => error);
  }

  getUserProvisionedResources() {
    return this.applicationServiceFacade
      .buildGetUserProvisionedResourcesRequest()
      .map((response:Response ) => response.json())
      .catch((error: any) => error);
  }

  createExploratoryEnvironment(data) {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateExploratoryEnvironmentRequest(body)
      .map((response:Response ) => response);
  }

  runExploratoryEnvironment(data) {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildRunExploratoryEnvironmentRequest(body)
      .map((response:Response ) => response);
  }

  suspendExploratoryEnvironment(data) {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildSuspendExploratoryEnvironmentRequest(body)
      .map((response:Response ) => response);
  }

  createComputationalResource(data) {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResourcesRequest(body)
      .map((response:Response ) => response);
  }

  suspendComputationalResource(data) {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildDeleteComputationalResourcesRequest(body)
      .map((response:Response ) => response);
  }
}
