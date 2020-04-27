import {Component, OnInit, AfterViewInit, Output, EventEmitter, OnDestroy} from '@angular/core';
import {SelectionModel} from '@angular/cdk/collections';
import {FlatTreeControl} from '@angular/cdk/tree';
import {MatTreeFlatDataSource, MatTreeFlattener} from '@angular/material/tree';
import {BucketBrowserService, TodoItemFlatNode, TodoItemNode} from '../../../core/services/bucket-browser.service';
import {BucketDataService} from '../bucket-data.service';
import {Subscription} from 'rxjs';


@Component({
  selector: 'dlab-folder-tree',
  templateUrl: './folder-tree.component.html',
  styleUrls: ['./folder-tree.component.scss']
})
export class FolderTreeComponent implements OnInit, OnDestroy {

  @Output() showFolderContent: EventEmitter<any> = new EventEmitter();
  private folderTreeSubs;
  private path = [];
  private selectedFolder: TodoItemFlatNode;
  private flatNodeMap = new Map<TodoItemFlatNode, TodoItemNode>();
  private nestedNodeMap = new Map<TodoItemNode, TodoItemFlatNode>();
  private selectedParent: TodoItemFlatNode | null = null;
  private newItemName = '';
  private subscriptions: Subscription = new Subscription();
  public treeControl: FlatTreeControl<TodoItemFlatNode>;
  private treeFlattener: MatTreeFlattener<TodoItemNode, TodoItemFlatNode>;
  public dataSource: MatTreeFlatDataSource<TodoItemNode, TodoItemFlatNode>;

  private checklistSelection = new SelectionModel<TodoItemFlatNode>(true /* multiple */);

  constructor(
    private bucketBrowserService: BucketBrowserService,
    private bucketDataService: BucketDataService,
    ) {
    this.treeFlattener = new MatTreeFlattener(this.transformer, this.getLevel, this.isExpandable, this.getChildren);
    this.treeControl = new FlatTreeControl<TodoItemFlatNode>(this.getLevel, this.isExpandable);
    this.dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

    this.subscriptions.add(bucketDataService._bucketData.subscribe(data => {
     if (data) {
       this.dataSource.data = data;
        const subject = this.dataSource._flattenedData;
       this.folderTreeSubs = subject.subscribe((subjectData) => {
          if (this.selectedFolder) {
            this.selectedFolder = subjectData.filter(v => v.item === this.selectedFolder.item && v.level === this.selectedFolder.level)[0];
          }
          this.expandAllParents(this.selectedFolder || subjectData[0]);
          this.showItem(this.selectedFolder || subjectData[0]);
        });
      }
    }));
  }

  getLevel = (node: TodoItemFlatNode) => node.level;

  isExpandable = (node: TodoItemFlatNode) => node.expandable;

  getChildren = (node: TodoItemNode): TodoItemNode[] => node.children;

  hasChild = (_: number, _nodeData: TodoItemFlatNode) => _nodeData.expandable;

  hasNoContent = (_: number, _nodeData: TodoItemFlatNode) => _nodeData.item === '';

  transformer = (node: TodoItemNode, level: number) => {
    const existingNode = this.nestedNodeMap.get(node);
    const flatNode = existingNode && existingNode.item === node.item
      ? existingNode
      : new TodoItemFlatNode();
    flatNode.item = node.item;
    flatNode.level = level;
    flatNode.expandable = !!node.children;
    this.flatNodeMap.set(flatNode, node);
    this.nestedNodeMap.set(node, flatNode);
    return flatNode;
  }


  ngOnInit() {
  }

  ngOnDestroy() {
    this.folderTreeSubs.unsubscribe();
    this.bucketDataService._bucketData.next([]);
  }

  private showItem(el) {
    if (el) {
      this.treeControl.expand(el);
      this.selectedFolder = el;
      const path = this.getPath(el);
      this.path = [];
      const data = {
        flatNode: el,
        element: this.flatNodeMap.get(el),
        path: path.join('/')
      };
      this.showFolderContent.emit(data);
    }
  }

  private getPath(el) {
    if (el) {
      if (this.path.length === 0) {
        this.path.unshift(el.item);
      }
      if (this.getParentNode(el) !== null) {
        this.path.unshift(this.getParentNode(el).item);
        this.getPath(this.getParentNode(el));
      }
      return this.path;
    }
  }

  private expandAllParents(el) {
    if (el) {
      this.treeControl.expand(el);
      if (this.getParentNode(el) !== null) {
        this.expandAllParents(this.getParentNode(el));
      }
    }
  }



  private descendantsAllSelected(node: TodoItemFlatNode): boolean {
    const descendants = this.treeControl.getDescendants(node);
    const descAllSelected = descendants.every(child =>
      this.checklistSelection.isSelected(child)
    );
    return descAllSelected;
  }

  private descendantsPartiallySelected(node: TodoItemFlatNode): boolean {
    const descendants = this.treeControl.getDescendants(node);
    const result = descendants.some(child => this.checklistSelection.isSelected(child));
    return result && !this.descendantsAllSelected(node);
  }

  private todoItemSelectionToggle(node: TodoItemFlatNode): void {
    this.checklistSelection.toggle(node);
  const descendants = this.treeControl.getDescendants(node);
  this.checklistSelection.isSelected(node)
? this.checklistSelection.select(...descendants)
    : this.checklistSelection.deselect(...descendants);

  // Force update for the parent
  descendants.every(child =>
  this.checklistSelection.isSelected(child)
);
  this.checkAllParentsSelection(node);
}

  private todoLeafItemSelectionToggle(node: TodoItemFlatNode): void {
    this.checklistSelection.toggle(node);
    this.checkAllParentsSelection(node);
  }

  private checkAllParentsSelection(node: TodoItemFlatNode): void {
    let parent: TodoItemFlatNode | null = this.getParentNode(node);
    while (parent) {
      this.checkRootNodeSelection(parent);
      parent = this.getParentNode(parent);
    }
  }

  private checkRootNodeSelection(node: TodoItemFlatNode): void {
    const nodeSelected = this.checklistSelection.isSelected(node);
    const descendants = this.treeControl.getDescendants(node);
    const descAllSelected = descendants.every(child =>
      this.checklistSelection.isSelected(child)
    );
    if (nodeSelected && !descAllSelected) {
      this.checklistSelection.deselect(node);
    } else if (!nodeSelected && descAllSelected) {
      this.checklistSelection.select(node);
    }
  }

  private getParentNode(node: TodoItemFlatNode): TodoItemFlatNode | null {
    const currentLevel = this.getLevel(node);

    if (currentLevel < 1) {
      return null;
    }

    const startIndex = this.treeControl.dataNodes.indexOf(node) - 1;

    for (let i = startIndex; i >= 0; i--) {
      const currentNode = this.treeControl.dataNodes[i];

      if (this.getLevel(currentNode) < currentLevel) {
        return currentNode;
      }
    }
    return null;
  }

  private addNewItem(node: TodoItemFlatNode, file, isFile, path) {
    const parentNode = this.flatNodeMap.get(node);
    this.bucketDataService.insertItem(parentNode!, file, isFile);
    this.treeControl.expand(node);
  }

  private saveNode(node: TodoItemFlatNode, itemValue: string) {
    const nestedNode = this.flatNodeMap.get(node);
    this.bucketDataService.updateItem(nestedNode!, itemValue);
  }
}
