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
    jupyter: {
      'py2': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
       },
      'py3': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
      },
      'r': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
        },
      },
      'scala': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
        },
      },
      '.DS_Store': {size: 0.01},
    },
    // folder: {
    //   folder: {
    //     folder: ['2008.cvs.bz2', 'airports.csv', 'carriers.csv'],
    //     folder2: []
    //   },
    //   'folder1': [],
    //   'file2': {size: 123.32},
    //   'file3': {size: 5.34},
    // },
    '2008.cvs.bz2': {size: 108.5},
    'airports.csv': {size: 0.23},
    'carriers.csv': {size: 0.04},
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
    jupyter: {
      'py2': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
      },
      'py3': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
          'part-00001-0e9a698b-9sa2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.03},
        },
      },
      'r': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
        },
      },
      'scala': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.02},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.06},
          'part-00001-0e9a698b-9ce2-4c86-a9eb-b624d0f292fc-c000.snappy.parquet': {size: 0.05},
        },
      },
      '.DS_Store': {size: 0.01},
    },
    '2008.cvs.bz2': {size: 108.5},
    'airports.csv': {size: 0.23},
    'carriers.csv': {size: 0.04},
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
