import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';

export class TodoItemNode {
  children: TodoItemNode[];
  item: string;
  id: string;
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
const TREE_DATA = {
  'dlab-local-shared-bucket': {
    FirsrFolder: {
      folder: {
        folder: ['2008.cvs.bz2', 'airports.csv', 'carriers.csv'],
        folder1: [],
        folder2: []
      },
      'folder1': [],
      'file2': null,
      'file3': null,
    },
    SecondFolder: [
      'file1',
      'file2',
      'file3'
    ]
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
    // Build the tree nodes from Json object. The result is a list of `TodoItemNode` with nested
    //     file node as children.
    const data = this.buildFileTree(TREE_DATA, 0);
    console.log(data);
    // Notify the change.
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
      if (value != null) {
        if (typeof value === 'object') {
          node.children = this.buildFileTree(value, level + 1);
        } else {
          node.item = value;
        }
      }

      return accumulator.concat(node);
    }, []);
  }

  /** Add an item to to-do list */
  insertItem(parent: TodoItemNode, name: string, isFile) {
    if (parent.children) {
      parent.children.push(isFile ? {item: name} as TodoItemNode : {item: name, children: []} as TodoItemNode);
      this.dataChange.next(this.data);
    }
  }

  updateItem(node: TodoItemNode, name: string) {
    node.item = name;
    this.dataChange.next(this.data);
  }

  uploadFile(parent: TodoItemNode, name: string) {
    if (parent.children) {
      parent.children.push({item: name, children: []} as TodoItemNode);
      this.dataChange.next(this.data);
    }
  }
}
