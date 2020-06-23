import {Component, Output, EventEmitter, OnDestroy, Input, OnInit} from '@angular/core';
import {FlatTreeControl} from '@angular/cdk/tree';
import {MatTreeFlatDataSource, MatTreeFlattener} from '@angular/material/tree';
import {BucketBrowserService, TodoItemFlatNode, TodoItemNode} from '../../../core/services/bucket-browser.service';
import {BucketDataService} from '../bucket-data.service';
import {Subscription} from 'rxjs';
import {FormControl, FormGroupDirective, NgForm, Validators} from '@angular/forms';
import {ErrorStateMatcher} from '@angular/material/core';
import {PATTERNS} from '../../../core/util';
import {ToastrService} from 'ngx-toastr';

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

export class FolderTreeComponent implements OnDestroy {

  @Output() showFolderContent: EventEmitter<any> = new EventEmitter();
  @Output() disableAll: EventEmitter<any> = new EventEmitter();
  @Input() folders;
  @Input() endpoint;

  private folderTreeSubs;
  private path = [];
  public selectedFolder: TodoItemFlatNode;
  private flatNodeMap = new Map<TodoItemFlatNode, TodoItemNode>();
  private nestedNodeMap = new Map<TodoItemNode, TodoItemFlatNode>();

  public folderCreating = false;
  private subscriptions: Subscription = new Subscription();
  public treeControl: FlatTreeControl<TodoItemFlatNode>;
  private treeFlattener: MatTreeFlattener<TodoItemNode, TodoItemFlatNode>;
  public dataSource: MatTreeFlatDataSource<TodoItemNode, TodoItemFlatNode>;

  constructor(
    public toastr: ToastrService,
    private bucketBrowserService: BucketBrowserService,
    public bucketDataService: BucketDataService,
  ) {
    this.treeFlattener = new MatTreeFlattener(this.transformer, this.getLevel, this.isExpandable, this.getChildren);
    this.treeControl = new FlatTreeControl<TodoItemFlatNode>(this.getLevel, this.isExpandable);
    this.dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
    this.subscriptions.add(this.bucketDataService._bucketData.subscribe(data => {
      if (data) {
        this.dataSource.data = data;
        const subject = this.dataSource._flattenedData;
        const subjectData = subject.getValue();
          if (this.selectedFolder) {
            this.selectedFolder = subjectData.filter(v => v.item === this.selectedFolder.item && v.level === this.selectedFolder.level)[0];
          }
          this.expandAllParents(this.selectedFolder || subjectData[0]);
          this.showItem(this.selectedFolder || subjectData[0]);
          if (this.selectedFolder && !this.bucketDataService.emptyFolder) {
            setTimeout(() => {
              const element = document.querySelector('.folder-item-line.active-item');
              element && element.scrollIntoView({ block: 'center', behavior: 'smooth' });
            }, 0);
          } else if (this.selectedFolder && this.bucketDataService.emptyFolder) {
            setTimeout(() => {
              const element = document.querySelector('#folder-form');
              element && element.scrollIntoView({ block: 'end', behavior: 'smooth' });
            }, 0);
          }
      }
    }));
    this.dataSource._flattenedData.subscribe();
  }

  getLevel = (node: TodoItemFlatNode) => node.level;

  isExpandable = (node: TodoItemFlatNode) => node.expandable;

  getChildren = (node: TodoItemNode): TodoItemNode[] => node.children;

  hasChild = (_: number, _nodeData: TodoItemFlatNode) => _nodeData.expandable;

  hasNoContent = (_: number, _nodeData: TodoItemFlatNode) => _nodeData.item === '' || _nodeData.item === 'ا';

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

  ngOnDestroy() {
    this.bucketDataService._bucketData.next([]);
    this.subscriptions.unsubscribe();
    this.bucketDataService.emptyFolder = null;
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


private addNewItem(node: TodoItemFlatNode, file, isFile) {
  const currNode = this.flatNodeMap.get(node);
  if (!currNode.object) {
    currNode.object = {bucket: currNode.item, object: ''};
  }
  const emptyFolderObject = currNode.object;
  if (emptyFolderObject.object.lastIndexOf('ا') !== emptyFolderObject.object.length - 1 || emptyFolderObject.object === '') {
    emptyFolderObject.object += 'ا';
  }
  this.bucketDataService.insertItem(currNode!, file, isFile, emptyFolderObject);
  this.treeControl.expand(node);
  setTimeout(() => {
    const element = document.querySelector('#folder-form');
    element && element.scrollIntoView({ block: 'end', behavior: 'smooth' });
  }, 0);
  }

  private removeItem(node: TodoItemFlatNode) {
    const parentNode = this.flatNodeMap.get(this.getParentNode(node));
    const childNode = this.flatNodeMap.get(node);
    this.bucketDataService.emptyFolder = null;
    this.bucketDataService.removeItem(parentNode!, childNode);
    this.resetForm();
  }

  private createFolder(node: TodoItemFlatNode, itemValue: string) {
    this.folderCreating = true;
    const parent = this.getParentNode(node);
    const flatParent = this.flatNodeMap.get(parent);
    let flatObject = flatParent.object.object;
    if (flatObject.indexOf('ا') === flatObject.length - 1) {
      flatObject = flatObject.substring(0, flatParent.object.object.length - 1);
    }
    const path = `${ flatParent.object && flatObject !== '/' ? flatObject : ''}${itemValue}/`;
    const bucket = flatParent.object ? flatParent.object.bucket : flatParent.item;
    // const formData = new FormData();
    // formData.append('file', '');
    // formData.append('object', path);
    // formData.append('bucket', bucket);
    // formData.append('endpoint', this.endpoint);

    this.bucketDataService.emptyFolder = null;
    this.bucketBrowserService.createFolder({
      'bucket': bucket,
      'folder': path,
      'endpoint': this.endpoint
    })
      .subscribe(_ => {
          this.bucketDataService.insertItem(flatParent, itemValue, false);
          // this.bucketDataService.refreshBucketdata(bucket, this.endpoint);
          this.toastr.success('Folder successfully created!', 'Success!');
          this.folderCreating = false;
          this.removeItem(node);
        }, error => {
          this.folderCreating = false;
          this.toastr.error(error.message || 'Folder creation error!', 'Oops!');
        });
  }

  private resetForm() {
    this.folderFormControl.setValue('');
    this.folderFormControl.updateValueAndValidity();
    this.folderFormControl.markAsPristine();
    this.disableAll.emit(false);
  }
}
