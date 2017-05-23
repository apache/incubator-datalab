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

import { Injectable } from '@angular/core';
import { Response } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import { ApplicationServiceFacade } from './';

@Injectable()
export class LibrariesInstallationService {

    constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

    public getGroupsList(): Observable<Response> {
        return this.applicationServiceFacade
        .buildGetGroupsList()
        .map(response => response);
    }

    public getAvailableLibrariesList(image, group, match): Observable<Response> {
        const body = '?image=' + image + '&group=' + group + '&start_with=' + match;
        return this.applicationServiceFacade
        .buildGetAvailableLibrariesList(body)
        .map(response => response);
    }
}