import { Component, OnInit } from '@angular/core';
import {Subscription} from 'rxjs';
import {OdahuDataService} from '../odahu-data.service';
import { MatTableDataSource } from '@angular/material/table';

import {ToastrService} from 'ngx-toastr';
import {MatDialog} from '@angular/material/dialog';
import {OdahuDeploymentService} from '../../../core/services';
import {OdahuActionDialogComponent} from '../../../shared/modal-dialog/odahu-action-dialog';

@Component({
  selector: 'odahu-grid',
  templateUrl: './odahu-grid.component.html',
  styleUrls: ['./odahu-grid.component.scss']
})
export class OdahuGridComponent implements OnInit {
  private odahuList: any[];
  private subscriptions: Subscription = new Subscription();
  public dataSource: MatTableDataSource<any>;
  displayedColumns: string[] = [ 'odahu-name', 'project', 'endpoint-url', 'odahu-status', 'actions'];

  constructor(
    private odahuDataService: OdahuDataService,
    private odahuDeploymentService: OdahuDeploymentService,
    public toastr: ToastrService,
    public dialog: MatDialog
  ) { }

  ngOnInit() {
    this.subscriptions.add(this.odahuDataService._odahuClasters.subscribe(
      (value) => {
        if (value) {
          this.odahuList = value;
          this.dataSource = new MatTableDataSource(value);
        }
      }));
  }

  private odahuAction(element: any, action: string) {
    this.dialog.open(OdahuActionDialogComponent, {data: {type: action, item: element}, panelClass: 'modal-sm'})
      .afterClosed().subscribe(result => {
        result && this.odahuDeploymentService.odahuAction(element,  action).subscribe(v =>
          this.odahuDataService.updateClasters(),
          error => this.toastr.error(`Odahu cluster ${action} failed!`, 'Oops!')
        ) ;
      }, error => this.toastr.error(error.message || `Odahu cluster ${action} failed!`, 'Oops!')
    );
  }
}
