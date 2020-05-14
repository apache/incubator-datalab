import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FlatTreeControl} from '@angular/cdk/tree';
import {MatTreeFlatDataSource, MatTreeFlattener} from '@angular/material/tree';


interface BucketNode {
  name: string;
  endpoint?: string;
  children?: BucketNode[];
}

/** Flat node with expandable and level information */
interface BucketFlatNode {
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
  private activeBacket: any;

  constructor() {
  }

  ngOnInit() {
    this.activeBucketName = this.openedBucket || '';
    this.dataSource.data = this.buckets;
    this.setActiveBucket();
  }

  public openBucketData(bucket) {
    this.dataSource['_treeControl'].collapseAll();
    this.setActiveBucket(bucket);
    this.emitActiveBucket.emit(bucket);
  }

  public setActiveBucket(bucket?) {
    this.activeBacket = bucket || this.dataSource._flattenedData.getValue().filter(v => v.name === this.openedBucket)[0];
    this.expandAllParents(this.activeBacket);
  }

  private expandAllParents(el) {
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
