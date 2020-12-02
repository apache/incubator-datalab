import { Component, OnInit } from '@angular/core';
import {Subscription} from 'rxjs';
import {LegionDeploymentDataService} from '../legion-deployment-data.service';
import { MatTableDataSource } from '@angular/material/table';
import {LegionDeploymentService} from '../../../core/services';
import {ToastrService} from 'ngx-toastr';
import {MatDialog} from '@angular/material/dialog';
import {OdahuActionDialogComponent} from '../../../shared/modal-dialog/odahu-action-dialog';

@Component({
  selector: 'legion-list',
  templateUrl: './legion-list.component.html',
  styleUrls: ['./legion-list.component.scss']
})
export class LegionListComponent implements OnInit {
  private legionClustersList: any[];
  private subscriptions: Subscription = new Subscription();
  public dataSource: MatTableDataSource<any>;
  displayedColumns: string[] = [ 'legion-name', 'project', 'endpoint-url', 'legion-status', 'actions'];

  constructor(
    private legionDeploymentDataService: LegionDeploymentDataService,
    private legionDeploymentService: LegionDeploymentService,
    public toastr: ToastrService,
    public dialog: MatDialog
  ) { }

  ngOnInit() {
    this.subscriptions.add(this.legionDeploymentDataService._legionClasters.subscribe(
      (value) => {
        if (value) {
          this.legionClustersList = value;
          this.dataSource = new MatTableDataSource(value);
        }
      }));
  }

  private odahuAction(element: any, action: string) {
    this.dialog.open(OdahuActionDialogComponent, {data: {type: action, item: element}, panelClass: 'modal-sm'})
      .afterClosed().subscribe(result => {
        result && this.legionDeploymentService.odahuAction(element,  action).subscribe(v =>
          this.legionDeploymentDataService.updateClasters(),
          error => this.toastr.error(`Odahu cluster ${action} failed!`, 'Oops!')
        ) ;
      }, error => this.toastr.error(error.message || `Odahu cluster ${action} failed!`, 'Oops!')
    );
  }
}
