import { Component, ViewChild, ViewEncapsulation } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'cost-details-dialog',
    templateUrl: 'cost-details-dialog.component.html',
    styleUrls: ['cost-details-dialog.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class CostDetailsDialogComponent {
  notebook: any;
  tooltip: boolean = false;

  @ViewChild('bindDialog') bindDialog;

  public open(params, notebook): void {
    this.tooltip = false;
    this.notebook = notebook;

    console.log(this.notebook);
    this.bindDialog.open(params);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  public isEllipsisActive($event): void {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }
}
