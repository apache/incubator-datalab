import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {ErrorUtils} from '../util';
import {ApplicationServiceFacade} from './applicationServiceFacade.service';

export class TodoItemNode {
  children: TodoItemNode[];
  item: string;
  id: string;
  size: number;
}

/** Flat to-do item node with expandable and level information */
export class TodoItemFlatNode {
  item: string;
  level: number;
  expandable: boolean;
}

/**
 * The Json object for to-do list data.
 */


const array = [{'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '4.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 11:36:36'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '5.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 11:56:46'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'Untitled', 'size': '5 bytes', 'creationDate': '13-4-2020 03:39:11'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'adasdas', 'size': '1 KB', 'creationDate': '15-4-2020 02:17:39'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'downloadTest.txt', 'size': '16 bytes', 'creationDate': '17-4-2020 03:47:47'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/', 'size': '11 bytes', 'creationDate': '13-4-2020 03:39:24'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/2.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 10:18:29'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/3.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 10:48:05'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/test.pem', 'size': '1 KB', 'creationDate': '15-4-2020 01:38:50'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder2/', 'size': '11 bytes', 'creationDate': '22-4-2020 08:46:13'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder3/1.txt', 'size': '18 bytes', 'creationDate': '22-4-2020 08:54:51'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'objectName', 'size': '5 bytes', 'creationDate': '14-4-2020 09:36:16'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'sss.txt', 'size': '52 bytes', 'creationDate': '17-4-2020 12:13:26'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test', 'size': '12 bytes', 'creationDate': '14-4-2020 04:55:02'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test.pem', 'size': '1 KB', 'creationDate': '14-4-2020 04:57:54'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test1', 'size': '52 bytes', 'creationDate': '17-4-2020 11:22:18'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test2', 'size': '52 bytes', 'creationDate': '17-4-2020 12:12:53'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'zzz', 'size': '12 bytes', 'creationDate': '14-4-2020 04:56:09'}];

const processFiles = (files, target) => {
  let pointer = target;
  files.forEach((file, index) => {
    if (!pointer[file]) {
      pointer[file] = {};
    }
    pointer = pointer[file];
  });

};

const processFolderArray = (acc, curr) => {
  const files = curr.object.split('/').filter(x => x.length > 0);
  processFiles(files, acc);
  return acc;
};

const convertToFolderTree = (data) => data
  .reduce(
    processFolderArray,
    {}
  );

const TREE_DATA = convertToFolderTree(array);


@Injectable({
  providedIn: 'root'
})
export class BucketBrowserService {
  public dataChange = new BehaviorSubject<TodoItemNode[]>([]);
  public serverData: any = [];
  get data(): TodoItemNode[] { return this.dataChange.value; }

  constructor(private applicationServiceFacade: ApplicationServiceFacade) {
    this.initialize();
  }

  public getBacketData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetBucketData()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public initialize() {
    let backetData = [];
    this.getBacketData().subscribe(v => {
      this.serverData = v;
      backetData = convertToFolderTree(v);
      const data = this.buildFileTree({'ofuks-1304-prj1-local-bucket': backetData}, 0);
      this.dataChange.next(data);
    });
    // this.serverData = array;
    // backetData = convertToFolderTree(this.serverData);
    // const data = this.buildFileTree({'ofuks-1304-prj1-local-bucket': backetData}, 0);
    // this.dataChange.next(data);
  }

  /**
   * Build the file structure tree. The `value` is the Json object, or a sub-tree of a Json object.
   * The return value is the list of `TodoItemNode`.
   */
  public buildFileTree(obj: {[key: string]: any}, level: number): TodoItemNode[] {
    return Object.keys(obj).reduce<TodoItemNode[]>((accumulator, key) => {
      const value = obj[key];
      const node = new TodoItemNode();
      node.item = key;
      if (Object.keys(value).length) {
        if (typeof value === 'object') {
          node.children = this.buildFileTree(value, level + 1);
        } else {
          node.item = value;
        }
      } else {
        node.size = this.serverData.filter(v => v.object.indexOf(node.item) !== -1)[0];
      }
      return accumulator.concat(node);
    }, []);
  }

  public insertItem(parent: TodoItemNode, name, isFile) {
    if (parent.children) {
      if (isFile) {
        parent.children.push(name as TodoItemNode);
      } else {
        parent.children.unshift({item: name, children: []} as TodoItemNode);
        this.dataChange.next(this.data);
      }
    }
  }

  public updateItem(node: TodoItemNode, file) {
    node.item = file;
    this.dataChange.next(this.data);
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
    const url = JSON.stringify(data)
    return this.applicationServiceFacade
      .buildDeleteFileFromBucket(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
  // initBucket(bucketType) {
  //   bucketType !== 'project' ? TREE_DATA = local : TREE_DATA = projecta;
  // }
}
