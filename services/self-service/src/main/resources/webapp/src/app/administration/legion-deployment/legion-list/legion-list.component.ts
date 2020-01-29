import { Component, OnInit } from '@angular/core';
import {Subscription} from 'rxjs';
import {LegionDeploymentDataService} from '../legion-deployment-data.service';
import { MatTableDataSource } from '@angular/material/table';
import {LegionDeploymentService} from '../../../core/services';

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
    private legionDeploymentService: LegionDeploymentService
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
    this.legionDeploymentService.odahuAction(element,  action).subscribe(v =>
      this.legionDeploymentDataService.updateClasters()
    );
  }

}
