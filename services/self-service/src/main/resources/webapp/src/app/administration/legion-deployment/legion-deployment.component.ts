import { Component, OnInit } from '@angular/core';
import {LegionDeploymentDataService} from './legion-deployment-data.service';
import {Subscription} from 'rxjs';
import {MatDialog} from '@angular/material/dialog';
import {ToastrService} from 'ngx-toastr';
import {CreateLegionClusterComponent} from './create-legion-claster/create-legion-cluster.component';
import {HealthStatusService, LegionDeploymentService} from '../../core/services';

export interface OdahuCluster {
  name: string;
  project: string;
  endpoint: string;
}

@Component({
  selector: 'dlab-legion-deployment',
  templateUrl: './legion-deployment.component.html',
  styleUrls: ['./legion-deployment.component.scss']
})
export class LegionDeploymentComponent implements OnInit {

  private legionClastersList: any[];
  private subscriptions: Subscription = new Subscription();
  private healthStatus;

  constructor(
    private legionDeploymentDataService: LegionDeploymentDataService,
    private dialog: MatDialog,
    public toastr: ToastrService,
    public legionDeploymentService: LegionDeploymentService,
    private healthStatusService: HealthStatusService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.subscriptions.add(this.legionDeploymentDataService._legionClasters.subscribe(
      (value) => {
        if (value) this.legionClastersList = value;
      }));
    this.refreshGrid();
  }

  public createLegionCluster(): void {
    this.dialog.open(CreateLegionClusterComponent, {  data: this.legionClastersList, panelClass: 'modal-lg' })
      .afterClosed().subscribe((result) => {
      result && this.getEnvironmentHealthStatus();
      this.refreshGrid();
      });
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => this.healthStatus = result);
  }

  public refreshGrid(): void {
    this.legionDeploymentDataService.updateClasters();
  }
}
