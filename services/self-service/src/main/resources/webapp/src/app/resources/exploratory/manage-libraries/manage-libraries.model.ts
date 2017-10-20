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

import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';

import { LibrariesInstallationService} from '../../../core/services';

interface Library {
    group: string;
    name: string;
    version: string;
}

export class ManageLibrariesModel {
    confirmAction: Function;
    notebook: any;

    public selectedLibs: Array<Library> = [];
    private continueWith: Function;
    private librariesInstallationService: LibrariesInstallationService;

    static getDefault(librariesInstallationService): ManageLibrariesModel {
        return new ManageLibrariesModel('', () => { }, () => { }, null, librariesInstallationService);
    }

    constructor(
        notebook: any,
        fnProcessResults: any,
        fnProcessErrors: any,
        continueWith: Function,
        librariesInstallationService: LibrariesInstallationService
    ) {
        this.notebook = notebook;
        this.continueWith = continueWith;
        this.librariesInstallationService = librariesInstallationService;
        this.prepareModel(fnProcessResults, fnProcessErrors);

        if (this.continueWith) this.continueWith();
    }

    public getLibrariesList(group: string, query: string): Observable<Response> {
        return this.librariesInstallationService
            .getAvailableLibrariesList({
                notebook_name: this.notebook.name,
                group: group,
                start_with: query
            });
    }

    public getInstalledLibrariesList(): Observable<Response> {
        return this.librariesInstallationService.getInstalledLibrariesList(this.notebook.name)
    }

    private installLibraries(retry?: Library): Observable<Response> {
        return this.librariesInstallationService.installLibraries({
            notebook_name: this.notebook.name,
            libs: (retry ? retry : this.selectedLibs)
        });
    }

    public isEmpty(obj) {
        if (obj) return Object.keys(obj).length === 0;
    }

    private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
        this.confirmAction = (retry?: Library) => this.installLibraries(retry)
            .subscribe(
                (response: Response) => fnProcessResults(response),
                (response: Response) => fnProcessErrors(response));
    }
}
