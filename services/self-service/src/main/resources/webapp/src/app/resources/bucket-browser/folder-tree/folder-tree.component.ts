import {Component, OnInit, AfterViewInit, Output, EventEmitter, OnDestroy, Input} from '@angular/core';
import {SelectionModel} from '@angular/cdk/collections';
import {FlatTreeControl} from '@angular/cdk/tree';
import {MatTreeFlatDataSource, MatTreeFlattener} from '@angular/material/tree';
import {BucketBrowserService, TodoItemFlatNode, TodoItemNode} from '../../../core/services/bucket-browser.service';
import {BucketDataService} from '../bucket-data.service';
import {Subscription} from 'rxjs';
import {FormControl, FormGroupDirective, NgForm, Validators} from '@angular/forms';
import {ErrorStateMatcher} from '@angular/material/core';
import {PATTERNS} from '../../../core/util';
import {ToastrService} from 'ngx-toastr';
import {HttpEventType, HttpResponse} from '@angular/common/http';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty));
  }
}

@Component({
  selector: 'dlab-folder-tree',
  templateUrl: './folder-tree.component.html',
  styleUrls: ['./folder-tree.component.scss']
})

export class FolderTreeComponent implements OnInit, OnDestroy {

  @Output() showFolderContent: EventEmitter<any> = new EventEmitter();
  @Output() disableAll: EventEmitter<any> = new EventEmitter();
  @Input() folders;
  @Input() endpoint;

  private folderTreeSubs;
  private path = [];
  private selectedFolder: TodoItemFlatNode;
  private flatNodeMap = new Map<TodoItemFlatNode, TodoItemNode>();
  private nestedNodeMap = new Map<TodoItemNode, TodoItemFlatNode>();

  private folderCreating = false;
  private subscriptions: Subscription = new Subscription();
  public treeControl: FlatTreeControl<TodoItemFlatNode>;
  private treeFlattener: MatTreeFlattener<TodoItemNode, TodoItemFlatNode>;
  public dataSource: MatTreeFlatDataSource<TodoItemNode, TodoItemFlatNode>;
  private checklistSelection = new SelectionModel<TodoItemFlatNode>(true /* multiple */);
  private folderCreationParent;

  constructor(
    public toastr: ToastrService,
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
          if (this.folderCreationParent) {
            this.folderCreationRefresh(this.folderCreationParent, '', false);
          } else {
            this.disableAll.emit(false);
          }
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
    this.bucketDataService._bucketData.next([]);
    this.subscriptions.unsubscribe();
  }

  folderFormControl = new FormControl('', [
    Validators.required,
    Validators.pattern(PATTERNS.folderRegex),
    this.duplicate.bind(this)
  ]);

  matcher = new MyErrorStateMatcher();

  private duplicate(control) {
    if (control && control.value) {
      const isDublicat = this.folders.slice(1).some(folder => folder.item === control.value);
      return isDublicat ? { isDuplicate: true } : null;
    }
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
        path: path.map(v => v.item).join('/'),
        pathObject: path
      };
      this.showFolderContent.emit(data);
    }
  }

  private getPath(el) {
    if (el) {
      if (this.path.length === 0) {
        this.path.unshift(el);
      }
      if (this.getParentNode(el) !== null) {
        this.path.unshift(this.getParentNode(el));
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
    this.folderCreationParent = node;
    this.folderCreationRefresh(node, file, isFile);
  }

  private folderCreationRefresh(node: TodoItemFlatNode, file, isFile) {
    const currNode = this.flatNodeMap.get(node);
    this.bucketDataService.insertItem(currNode!, file, isFile);
    this.treeControl.expand(node);
    setTimeout(() => {
      const element = document.querySelector('#folder-form');
      element && element.scrollIntoView({ block: 'end', behavior: 'smooth' });
    }, 0);
  }

  private removeItem(node: TodoItemFlatNode) {
    const parentNode = this.flatNodeMap.get(this.getParentNode(node));
    this.bucketDataService.removeItem(parentNode!);
    this.resetForm();
  }

  private saveNode(node: TodoItemFlatNode, itemValue: string) {
    this.folderCreating = true;
    const parent = this.getParentNode(node);
    const flatParent = this.flatNodeMap.get(parent);
    const path = `${ flatParent.object ? flatParent.object.object : ''}${itemValue}/`;
    const bucket = flatParent.object ? flatParent.object.bucket : flatParent.item;
    const formData = new FormData();
    formData.append('file', '');
    formData.append('object', path);
    formData.append('bucket', bucket);
    formData.append('endpoint', this.endpoint);
    this.bucketBrowserService.uploadFile(formData)
      .subscribe((event) => {
      if (event instanceof HttpResponse) {
          this.bucketDataService.refreshBucketdata(bucket, this.endpoint);
          this.toastr.success('Folder successfully created!', 'Success!');
          this.resetForm();
          this.folderFormControl.updateValueAndValidity();
          this.folderFormControl.markAsPristine();
          this.folderCreating = false;
          this.folderCreationParent = null;
        }
        }, error => {
          this.toastr.error(error.message || 'Folder creation error!', 'Oops!');
          this.folderCreating = false;
          this.folderCreationParent = null;
        }
      );
  }

  private resetForm() {
    this.folderFormControl.setValue('');
    this.folderFormControl.updateValueAndValidity();
    this.folderFormControl.markAsPristine();
    this.disableAll.emit(false);
  }
}
