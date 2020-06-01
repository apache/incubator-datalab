/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject} from 'rxjs';
import {BucketBrowserService, TodoItemNode} from '../../core/services/bucket-browser.service';


const array = [{'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder11/', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:36:36'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder11/dsafaraorueajkegrgavhsfnvgahsfgsdjfhagsdjfg497frgfhsdajfsgdafjsxzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzdvcfdvbgffgsdfgdsafaraorueajkegrgavhsfnvgahsfgsdjfhagsdjfg497frgfhsdajfsgdafjsxzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzdvcfdvbgffgsdfgdsfgsdfgggggg', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:36:36'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '51.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:56:46'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'Untitlsed', 'size': '5 bytes', 'lastModifiedDate': '13-4-2020 03:39:11'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'adassdas', 'size': '1 KB', 'lastModifiedDate': '15-4-2020 02:17:39'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'downloadTest.txt', 'size': '16 bytes', 'lastModifiedDate': '17-4-2020 03:47:47'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/', 'size': '11 bytes', 'lastModifiedDate': '13-4-2020 03:39:24'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/2.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 10:18:29'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder31/3.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 10:48:05'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/folder1/folder1/folder1/folder1/test.pem', 'size': '1 KB', 'lastModifiedDate': '15-4-2020 01:38:50'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder12/', 'size': '11 bytes', 'lastModifiedDate': '22-4-2020 08:46:13'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder3/1.txt', 'size': '18 bytes', 'lastModifiedDate': '22-4-2020 08:54:51'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'objectName', 'size': '5 bytes', 'lastModifiedDate': '14-4-2020 09:36:16'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'sss.txt', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 12:13:26'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test', 'size': '12 bytes', 'lastModifiedDate': '14-4-2020 04:55:02'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test.pem', 'size': '1 KB', 'lastModifiedDate': '14-4-2020 04:57:54'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test1', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 11:22:18'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test2', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 12:12:53'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'zzz', 'size': '12 bytes', 'lastModifiedDate': '14-4-2020 04:56:09'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '4.txt/dsafaraorueajkegrgavhsfnvgahsfgsdjfhagsdjfg497frgfhsdajfsgdafj', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:36:36'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '5.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:56:46'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'Untitled', 'size': '5 bytes', 'lastModifiedDate': '13-4-2020 03:39:11'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'adasdas', 'size': '1 KB', 'lastModifiedDate': '15-4-2020 02:17:39'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'downloadTest.txt', 'size': '16 bytes', 'lastModifiedDate': '17-4-2020 03:47:47'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/', 'size': '11 bytes', 'lastModifiedDate': '13-4-2020 03:39:24'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/2.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 10:18:29'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder31/3.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 10:48:05'},
{'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '3.jpg', 'size': '5 bytes', 'lastModifiedDate': '14-4-2020 09:36:16'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '2.jpg', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 12:13:26'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '1test', 'size': '112 bytes', 'lastModifiedDate': '14-4-2020 04:55:02'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test.pem', 'size': '1 KB', 'lastModifiedDate': '14-4-2020 04:57:54'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test11', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 11:22:18'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test2', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 12:12:53'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'zzsz', 'size': '12 bytes', 'lastModifiedDate': '14-4-2020 04:56:09'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '4.txt/dsafaraorueajkegrgavhsfnvgahsfgsdjfhagsdjfg497frgfhsdajfsgdafj', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:36:36'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '5.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 11:56:46'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'Untitled', 'size': '5 bytes', 'lastModifiedDate': '13-4-2020 03:39:11'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'adasdas', 'size': '1 KB', 'lastModifiedDate': '15-4-2020 02:17:39'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'downloadTest.txt', 'size': '16 bytes', 'lastModifiedDate': '17-4-2020 03:47:47'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1212/', 'size': '11 bytes', 'lastModifiedDate': '13-4-2020 03:39:24'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/2.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 10:18:29'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/3.txt', 'size': '18 bytes', 'lastModifiedDate': '21-4-2020 10:48:05'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'fol2der1/folder1/folder1/folder1/2folder1/test.pem', 'size': '1 KB', 'lastModifiedDate': '15-4-2020 01:38:50'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder2/', 'size': '11 bytes', 'lastModifiedDate': '22-4-2020 08:46:13'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder3/1.txt', 'size': '18 bytes', 'lastModifiedDate': '22-4-2020 08:54:51'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'objectName', 'size': '5 bytes', 'lastModifiedDate': '14-4-2020 09:36:16'},
{'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '5.jpg', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 12:13:26'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test', 'size': '12 bytes', 'lastModifiedDate': '14-4-2020 04:55:02'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test.pem', 'size': '1 KB', 'lastModifiedDate': '14-4-2020 04:57:54'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test1', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 11:22:18'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test2', 'size': '52 bytes', 'lastModifiedDate': '17-4-2020 12:12:53'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'zzz', 'size': '12 bytes', 'lastModifiedDate': '14-4-2020 04:56:09'}];

@Injectable()
export class BucketDataService {
  public _bucketData = new BehaviorSubject<any>(null);
  public serverData: any = [];
  get data(): TodoItemNode[] { return this._bucketData.value; }
  emptyFolder = null;
  constructor(
    private bucketBrowserService: BucketBrowserService,
  ) {
  }

  public refreshBucketdata(bucket, endpoint) {
    let backetData = [];
     this.bucketBrowserService.getBucketData(bucket, endpoint).subscribe(v => {
     const copiedData = JSON.parse(JSON.stringify(v));

     this.serverData = v;
     if (this.emptyFolder) {
       copiedData.unshift(this.emptyFolder);
     }
     backetData = this.convertToFolderTree(copiedData);
     const data = this.buildFileTree({[bucket]: backetData}, 0);
     this._bucketData.next(data);
     });
      // this.serverData = array;
      // backetData = this.convertToFolderTree(array);
      // const data = this.buildFileTree({[bucket]: backetData}, 0);
      // this._bucketData.next(data);
  }

  public buildFileTree(obj: {[key: string]: any}, level: number): TodoItemNode[] {
      return Object.keys(obj).reduce<TodoItemNode[]>((accumulator, key) => {
        if (key === '') {
          return accumulator;
        }
        const value = obj[key];
        const node = new TodoItemNode();
        node.item = key;
        if (value === '') {
          node.children = this.buildFileTree({}, level + 1);
          return accumulator.concat(node);
        }
        if (Object.keys(value).filter(v => v !== 'obj').length > 0) {
          if (typeof value === 'object') {
            node.object = value.obj || {'bucket': node.item, 'object': '', 'size': '', 'lastModifiedDate': ''};
            delete value.obj;
            node.children = this.buildFileTree(value, level + 1);
          } else {
            node.item = value;
          }
        } else {
          node.object = value.obj;
        }
        return accumulator.concat(node);
      }, []);
    }

  public insertItem(parent: TodoItemNode, name, isFile, emptyFolderObj?) {
    if (parent.children) {
        if (isFile) {
          parent.children.unshift(name as TodoItemNode);
        } else {
          if (name) {
            const child = {item: name, children: [], object: JSON.parse(JSON.stringify(parent.object))};
            child.object.object = child.object.object.slice(0, -1) + child.item + '/';
            parent.children.unshift(child as TodoItemNode);
          } else {
            parent.children.unshift({item: '', children: [], object: {}} as TodoItemNode);
            this.emptyFolder = emptyFolderObj;
            this._bucketData.next(this.data);
          }
        }
      }
    }

  public updateItem(node: TodoItemNode, file) {
      node.item = file;
      this._bucketData.next(this.data);
    }

  public removeItem(parent, child) {
     parent.children.splice( parent.children.indexOf(child), 1);
     this._bucketData.next(this.data);
    }

    public processFiles = (files, target, object) => {
      let pointer = target;
      files.forEach((file) => {
        if (!pointer[file]) {
          pointer[file] = {};
        }
        pointer = pointer[file];
        if (!pointer.obj) {
          pointer.obj = object;
        }
      });
    };

    public processFolderArray = (acc, curr) => {
      const files = curr.object.split('/');
      this.processFiles(files, acc, curr);

      return acc;
    };

    public convertToFolderTree = (data) => {
      const finalData = data.reduce(this.processFolderArray, {});
      if (Object.keys(finalData).length === 0) {
        return '';
      }
      return finalData;
    }
}
