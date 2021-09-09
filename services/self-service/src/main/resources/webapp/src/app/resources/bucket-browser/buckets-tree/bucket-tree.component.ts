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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FlatTreeControl} from '@angular/cdk/tree';
import {MatTreeFlatDataSource, MatTreeFlattener} from '@angular/material/tree';

interface BucketNode {
  name: string;
  endpoint?: string;
  children?: BucketNode[];
}

interface BucketFlatNode {
  expandable: boolean;
  name: string;
  level: number;
}

@Component({
  selector: 'datalab-bucket-tree',
  templateUrl: './bucket-tree.component.html',
  styleUrls: ['./bucket-tree.component.scss']
})

export class BucketTreeComponent implements OnInit {
  @Output() emitActiveBucket: EventEmitter<{}> = new EventEmitter();
  @Input() openedBucket: string;
  @Input() buckets: BucketNode[];

  private _transformer = (node: BucketNode, level: number) => {
    return {
      expandable: !!node.children && node.children.length > 0,
      name: node.name,
      endpoint: node.endpoint,
      level: level,
    };
  }

  treeControl = new FlatTreeControl<BucketFlatNode>(
    node => node.level, node => node.expandable);

  treeFlattener = new MatTreeFlattener(
    this._transformer, node => node.level, node => node.expandable, node => node.children);

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
  private activeBucketName: string;
  public activeBacket: any;

  constructor() {}

  ngOnInit() {
    this.activeBucketName = this.openedBucket || '';
    this.dataSource.data = this.buckets;
    this.setActiveBucket();
  }

  public openBucketData(bucket): void {
    this.dataSource['_treeControl'].collapseAll();
    this.setActiveBucket(bucket);
    this.emitActiveBucket.emit(bucket);
  }

  public setActiveBucket(bucket?): void {
    this.activeBacket = bucket || this.dataSource._flattenedData.getValue().filter(v => v.name === this.openedBucket)[0];
    this.expandAllParents(this.activeBacket);
  }

  public toggleProject(el, isExpanded): void {
    isExpanded ? this.treeControl.collapse(el) : this.treeControl.expand(el);
  }

  private expandAllParents(el): void {
    if (el) {
      this.treeControl.expand(el);
      if (this.getParentNode(el) !== null) {
        this.expandAllParents(this.getParentNode(el));
      }
    }
  }

  private getParentNode(node: BucketFlatNode): BucketFlatNode | null {
    const currentLevel = node.level;
    if (currentLevel < 1) {
      return null;
    }

    const startIndex = this.treeControl.dataNodes.indexOf(node) - 1;

    for (let i = startIndex; i >= 0; i--) {
      const currentNode = this.treeControl.dataNodes[i];

      if (currentNode.level < currentLevel) {
        return currentNode;
      }
    }
    return null;
  }

  public hasChild = (_: number, node: BucketFlatNode) => node.expandable;
}
