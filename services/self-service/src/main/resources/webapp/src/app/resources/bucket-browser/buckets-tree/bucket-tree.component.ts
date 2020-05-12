import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FlatTreeControl} from '@angular/cdk/tree';
import {MatTreeFlatDataSource, MatTreeFlattener} from '@angular/material/tree';

interface FoodNode {
  name: string;
  children?: FoodNode[];
}

const TREE_DATA: FoodNode[] = [
      {
        name: 'ProjectA(local)',
        children: [
          {name: 'vi-aws-11-05-projectb-local-bucket.'},
          {name: 'ad-aws-11-05-projectb-local-bucket.'},
        ]
      }, {
        name: 'ProjectA(local1)',
        children: [
          {name: 'rt-aws-11-05-projectb-local-bucket.'},
          {name: 'rt-aws-11-05-projectb-local-bucket.'},
        ]
      },
    ];

/** Flat node with expandable and level information */
interface ExampleFlatNode {
  expandable: boolean;
  name: string;
  level: number;
}



@Component({
  selector: 'dlab-bucket-tree',
  templateUrl: './bucket-tree.component.html',
  styleUrls: ['./bucket-tree.component.scss']
})

export class BucketTreeComponent implements OnInit {
  public activeBucket;

  @Output() emitActiveBucket: EventEmitter<{}> = new EventEmitter();
  @Input() openedBucket: string;

  private _transformer = (node: FoodNode, level: number) => {
    return {
      expandable: !!node.children && node.children.length > 0,
      name: node.name,
      level: level,
    };
  }

  treeControl = new FlatTreeControl<ExampleFlatNode>(
    node => node.level, node => node.expandable);

  treeFlattener = new MatTreeFlattener(
    this._transformer, node => node.level, node => node.expandable, node => node.children);

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
  private activeBucketName: string;

  constructor() {
    this.dataSource.data = TREE_DATA;
  }

  ngOnInit() {
    this.activeBucketName = this.openedBucket || '';
    // console.log(this.activeBucketName);
    // console.log(...this.dataSource._flattenedData.getValue().filter(v => v.name === this.activeBucketName));
  }

  public openBucketData(bucket) {
    this.treeControl.expand(bucket);
    this.activeBucket = bucket;
    // console.log(bucket);
  }

  hasChild = (_: number, node: ExampleFlatNode) => node.expandable;
}
