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
      'rstudio': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-58f64e98-3cbe-4015-a85a-fd500b3f3643-c000.snappy.parquet': {size: 0.01},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-2a5e4521-9bcd-4ca4-be08-531bb9d91459-c000.snappy.parquet': {size: 0.01},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-a57515f7-591c-44cb-9fbb-2ce24628e399-c000.snappy.parquet': {size: 0.06},
          'part-00001-a57515f7-591c-44cb-9fbb-2ce24628e399-c000.snappy.parquet': {size: 0.05},
        },
      },
      'zeppelin': {
        'py2': {
          'airports': {
            '_SUCCESS': {size: 0.01},
            'part-00000-4fe7b380-111d-4c86-acf8-822924cd2ff9-c000.snappy.parquet': {size: 0.02},
            'part-00001-4fe7b380-111d-4c86-acf8-822924cd2ff9-c000.snappy.parquet': {size: 0.03},
          },
          'carriers': {
            '_SUCCESS': {size: 0.01},
            'part-00000-13d1d478-a106-446d-868e-e8b8d1a4b560-c000.snappy.parquet': {size: 0.02},
            'part-00001-13d1d478-a106-446d-868e-e8b8d1a4b560-c000.snappy.parquet': {size: 0.03},
          },
          'flight': {
            '_SUCCESS': {size: 0.01},
            'part-00000-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.06},
            'part-00001-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.05},
            'part-00002-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.03},
            'part-00003-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.01},
          },
        },
      },
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
    'jupyter': {
      'py2': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-72ed6b1b-1fae-4f05-8b18-48f7b2bc2247-c000.snappy.parquet': {size: 0.02},
          'part-00001-72ed6b1b-1fae-4f05-8b18-48f7b2bc2247-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-8c78a07b-0a94-43dc-a795-6bc905a1e19c-c000.snappy.parquet': {size: 0.02},
          'part-00001-8c78a07b-0a94-43dc-a795-6bc905a1e19c-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-6ebf941f-8804-4a0f-8910-45893e0e0ad6-c000.snappy.parquet': {size: 0.06},
          'part-00001-6ebf941f-8804-4a0f-8910-45893e0e0ad6-c000.snappy.parquet': {size: 0.05},
          'part-00002-6ebf941f-8804-4a0f-8910-45893e0e0ad6-c000.snappy.parquet': {size: 0.03},
          'part-00003-6ebf941f-8804-4a0f-8910-45893e0e0ad6-c000.snappy.parquet': {size: 0.01},
        },
      },
      'py3': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-054c8aea-36d6-42ae-b5aa-ebca86783fe8-c000.snappy.parquet': {size: 0.02},
          'part-00001-054c8aea-36d6-42ae-b5aa-ebca86783fe8-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-ace256f9-6e6a-4830-9615-f10a5d4944a7-c000.snappy.parquet': {size: 0.02},
          'part-00001-ace256f9-6e6a-4830-9615-f10a5d4944a7-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-acf6bc7d-b262-4363-b4a5-996eda847206-c000.snappy.parquet': {size: 0.06},
          'part-00001-acf6bc7d-b262-4363-b4a5-996eda847206-c000.snappy.parquet': {size: 0.05},
          'part-00002-acf6bc7d-b262-4363-b4a5-996eda847206-c000.snappy.parquet': {size: 0.03},
          'part-00003-acf6bc7d-b262-4363-b4a5-996eda847206-c000.snappy.parquet': {size: 0.03},
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
    },
    'rstudio': {
      'airports': {
        '_SUCCESS': {size: 0.01},
        'part-00000-58f64e98-3cbe-4015-a85a-fd500b3f3643-c000.snappy.parquet': {size: 0.01},
      },
      'carriers': {
        '_SUCCESS': {size: 0.01},
        'part-00000-2a5e4521-9bcd-4ca4-be08-531bb9d91459-c000.snappy.parquet': {size: 0.01},
      },
      'flight': {
        '_SUCCESS': {size: 0.01},
        'part-00000-a57515f7-591c-44cb-9fbb-2ce24628e399-c000.snappy.parquet': {size: 0.06},
        'part-00001-a57515f7-591c-44cb-9fbb-2ce24628e399-c000.snappy.parquet': {size: 0.05},
      },
    },
    'zeppelin': {
      'py2': {
        'airports': {
          '_SUCCESS': {size: 0.01},
          'part-00000-4fe7b380-111d-4c86-acf8-822924cd2ff9-c000.snappy.parquet': {size: 0.02},
          'part-00001-4fe7b380-111d-4c86-acf8-822924cd2ff9-c000.snappy.parquet': {size: 0.03},
        },
        'carriers': {
          '_SUCCESS': {size: 0.01},
          'part-00000-13d1d478-a106-446d-868e-e8b8d1a4b560-c000.snappy.parquet': {size: 0.02},
          'part-00001-13d1d478-a106-446d-868e-e8b8d1a4b560-c000.snappy.parquet': {size: 0.03},
        },
        'flight': {
          '_SUCCESS': {size: 0.01},
          'part-00000-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.06},
          'part-00001-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.05},
          'part-00002-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.03},
          'part-00003-f35fcc5a-e393-4d47-b5f2-47a12ca597dc-c000.snappy.parquet': {size: 0.01},
        },
      },
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
