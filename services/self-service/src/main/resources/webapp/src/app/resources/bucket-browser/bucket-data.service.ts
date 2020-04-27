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
import { BehaviorSubject, of } from 'rxjs';
import {BucketBrowserService, TodoItemNode} from '../../core/services/bucket-browser.service';

const array = [{'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '4.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 11:36:36'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': '5.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 11:56:46'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'Untitled', 'size': '5 bytes', 'creationDate': '13-4-2020 03:39:11'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'adasdas', 'size': '1 KB', 'creationDate': '15-4-2020 02:17:39'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'downloadTest.txt', 'size': '16 bytes', 'creationDate': '17-4-2020 03:47:47'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/', 'size': '11 bytes', 'creationDate': '13-4-2020 03:39:24'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/2.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 10:18:29'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/3.txt', 'size': '18 bytes', 'creationDate': '21-4-2020 10:48:05'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder1/test.pem', 'size': '1 KB', 'creationDate': '15-4-2020 01:38:50'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder2/', 'size': '11 bytes', 'creationDate': '22-4-2020 08:46:13'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'folder3/1.txt', 'size': '18 bytes', 'creationDate': '22-4-2020 08:54:51'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'objectName', 'size': '5 bytes', 'creationDate': '14-4-2020 09:36:16'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'sss.txt', 'size': '52 bytes', 'creationDate': '17-4-2020 12:13:26'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test', 'size': '12 bytes', 'creationDate': '14-4-2020 04:55:02'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test.pem', 'size': '1 KB', 'creationDate': '14-4-2020 04:57:54'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test1', 'size': '52 bytes', 'creationDate': '17-4-2020 11:22:18'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'test2', 'size': '52 bytes', 'creationDate': '17-4-2020 12:12:53'}, {'bucket': 'ofuks-1304-prj1-local-bucket', 'object': 'zzz', 'size': '12 bytes', 'creationDate': '14-4-2020 04:56:09'}];


@Injectable()
export class BucketDataService {
  public _bucketData = new BehaviorSubject<any>(null);
  private serverData: any = [];
  get data(): TodoItemNode[] { return this._bucketData.value; }
  constructor(
    private bucketBrowserService: BucketBrowserService,
  ) {
  }

  public refreshBucketdata(bucket, endpoint) {
    let backetData = [];
    // this.bucketBrowserService.getBacketData(bucket, endpoint).subscribe(v => {
    //   this.serverData = v;
    //   backetData = this.convertToFolderTree(v);
    //   const data = this.buildFileTree({[bucket]: backetData}, 0);
    //   this._bucketData.next(data);
    // });

    this.serverData = array;
    backetData = this.convertToFolderTree(array);
    const data = this.buildFileTree({[bucket]: backetData}, 0);
    this._bucketData.next(data);
  }

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
          this._bucketData.next(this.data);
        }
      }
    }

  public updateItem(node: TodoItemNode, file) {
      node.item = file;
      this._bucketData.next(this.data);
    }

    public processFiles = (files, target) => {
      let pointer = target;
      files.forEach((file, index) => {
        if (!pointer[file]) {
          pointer[file] = {};
        }
        pointer = pointer[file];
      });

    };

    public processFolderArray = (acc, curr) => {
      const files = curr.object.split('/').filter(x => x.length > 0);
      this.processFiles(files, acc);
      return acc;
    };

    public convertToFolderTree = (data) => data
      .reduce(
        this.processFolderArray,
        {}
      )

}
