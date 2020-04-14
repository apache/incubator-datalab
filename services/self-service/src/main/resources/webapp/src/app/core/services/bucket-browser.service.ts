import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';

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
let TREE_DATA = {};
const local = {
  'dlab-local-shared-bucket': {
    // folder: {
    //   folder: {
    //     folder: ['2008.cvs.bz2', 'airports.csv', 'carriers.csv'],
    //     folder2: []
    //   },
    //   'folder1': [],
    //   'file2': {size: 123.32},
    //   'file3': {size: 5.34},
    // },
    '2008.cvs.bz2': {size: 125.34},
    'airports.csv': {size: 33.12},
    'carriers.csv': {size: 46.13},
  }
};
const projecta = {
  'dlab-projecta-local-bucket': {
    // folder: {
    //   folder: {
    //     folder: ['2008.cvs.bz2', 'airports.csv', 'carriers.csv'],
    //     folder2: []
    //   },
    //   'folder1': [],
    //   'file2': {size: 123.32},
    //   'file3': {size: 5.34},
    // },
    '2008.cvs.bz2': {size: 125.34},
    'airports.csv': {size: 33.12},
    'carriers.csv': {size: 46.13},
  }
};

@Injectable({
  providedIn: 'root'
})
export class BucketBrowserService {
  dataChange = new BehaviorSubject<TodoItemNode[]>([]);

  get data(): TodoItemNode[] { return this.dataChange.value; }

  constructor() {
    this.initialize();
  }

  initialize() {
    const data = this.buildFileTree(TREE_DATA, 0);
    this.dataChange.next(data);
  }

  /**
   * Build the file structure tree. The `value` is the Json object, or a sub-tree of a Json object.
   * The return value is the list of `TodoItemNode`.
   */
  buildFileTree(obj: {[key: string]: any}, level: number): TodoItemNode[] {
    return Object.keys(obj).reduce<TodoItemNode[]>((accumulator, key) => {
      const value = obj[key];
      const node = new TodoItemNode();
      node.item = key;
      if (!value.size) {
        if (typeof value === 'object') {
          node.children = this.buildFileTree(value, level + 1);
        } else {
          node.item = value;
        }
      } else {
        node.size = value.size;
      }

      return accumulator.concat(node);
    }, []);
  }

  insertItem(parent: TodoItemNode, name, isFile) {
    if (parent.children) {
      if (isFile) {
        parent.children.push(name as TodoItemNode);
      } else {
        parent.children.unshift({item: name, children: []} as TodoItemNode);
        this.dataChange.next(this.data);
      }
    }
  }

  updateItem(node: TodoItemNode, file) {
    node.item = file;
    this.dataChange.next(this.data);
  }

  uploadFile(parent: TodoItemNode, name: string) {
    if (parent.children) {
      parent.children.push({item: name, children: []} as TodoItemNode);
      this.dataChange.next(this.data);
    }
  }

  initBucket(bucketType) {
    bucketType !== 'project' ? TREE_DATA = local : TREE_DATA = projecta;
  }
}
