import { Injectable } from '@angular/core';
import {Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {ErrorUtils} from '../util';
import {ApplicationServiceFacade} from './applicationServiceFacade.service';

export class TodoItemNode {
  children: TodoItemNode[];
  item: string;
  id: string;
  size: number;
}

export class TodoItemFlatNode {
  item: string;
  level: number;
  expandable: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BucketBrowserService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {
  }

  public getBacketData(bucket, endpoint): Observable<{}> {
    const url = `/${bucket}/endpoint/${endpoint}`
    return this.applicationServiceFacade
      .buildGetBucketData(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public downloadFile(data) {
    return this.applicationServiceFacade
      .buildDownloadFileFromBucket(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public uploadFile(data) {
    return this.applicationServiceFacade
      .buildUploadFileToBucket(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public deleteFile(data) {
    const url = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildDeleteFileFromBucket(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
