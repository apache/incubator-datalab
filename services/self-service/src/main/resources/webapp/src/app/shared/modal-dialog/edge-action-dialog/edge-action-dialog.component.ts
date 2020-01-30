import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';


@Component({
  selector: 'edge-action-dialog',
  template: `
  <div id="dialog-box" *ngIf="data.type">
    <header class="dialog-header">
      <h4 class="modal-title"><span class="action">{{data.type | titlecase}}</span> edge node</h4>
      <button type="button" class="close" (click)="closeModal()">&times;</button>
    </header>
      <div mat-dialog-content class="content message mat-dialog-content">
          <h3 class="strong">Select the items you want to {{data.type}}</h3>
          <ul class="endpoint-list scrolling-content">
              <li *ngFor="let endpoint of data.item" class="endpoint-list-item">
                  <label class="strong">
                      <input type="checkbox" [(ngModel)]="endpoint.checked" (change)="endpointAction()">
                      {{endpoint.name}}
                  </label>
              </li>
          </ul>

      <p class="m-top-20 action-text"><span class="strong">Do you want to proceed?</span></p>

      <div class="text-center m-top-30 m-bott-30">
        <button type="button" class="butt" mat-raised-button (click)="closeModal()">No</button>
        <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(endpointsNewStatus)" [disabled]="!endpointsNewStatus.length">Yes</button>
      </div>
      </div>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; }
    .info { color: #35afd5; }
    .info .confirm-dialog { color: #607D8B; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    .endpoint-list{text-align: left; margin-top: 30px}
    .endpoint-list-item{padding: 5px 0}
    .action{text-transform: capitalize}
    .action-text { text-align: center; }
    .scrolling-content{overflow-y: auto; max-height: 200px; }
    label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif; cursor: pointer; display: flex; align-items: center;}
    label input {margin-top: 2px; margin-right: 5px;}
    .scrolling-content{overflow-y: auto; max-height: 200px;}
  `]
})

export class EdgeActionDialogComponent {
  public endpointsNewStatus: Array<object> = [];
  constructor(
    public dialogRef: MatDialogRef<EdgeActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
  }

  private closeModal() {
    this.dialogRef.close();
    this.data.item.forEach(endpoint => endpoint.checked = false);
  }

  public endpointAction() {
    this.endpointsNewStatus = this.data.item.filter(endpoint => endpoint.checked);
  }
}
