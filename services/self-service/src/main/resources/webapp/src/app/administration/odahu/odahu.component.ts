import { Component, OnInit } from '@angular/core';
import {OdahuDataService} from './odahu-data.service';
import {Subscription} from 'rxjs';
import {MatDialog} from '@angular/material/dialog';
import {ToastrService} from 'ngx-toastr';
import {CreateOdahuClusterComponent} from './create-odahu-claster/create-odahu-cluster.component';
import {HealthStatusService, OdahuDeploymentService} from '../../core/services';

export interface OdahuCluster {
  name: string;
  project: string;
  endpoint: string;
}

@Component({
  selector: 'datalab-odahu',
  templateUrl: './odahu.component.html',
  styleUrls: ['./odahu.component.scss']
})
export class OdahuComponent implements OnInit {

  private odahuList: any[];
  private subscriptions: Subscription = new Subscription();
  private healthStatus;

  constructor(
    private odahuDataService: OdahuDataService,
    private dialog: MatDialog,
    public toastr: ToastrService,
    public odahuDeploymentService: OdahuDeploymentService,
    private healthStatusService: HealthStatusService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.subscriptions.add(this.odahuDataService._odahuClasters.subscribe(
      (value) => {
        if (value) this.odahuList = value;
      }));
    this.refreshGrid();
  }

  public createOdahuCluster(): void {
    this.dialog.open(CreateOdahuClusterComponent, {  data: this.odahuList, panelClass: 'modal-lg' })
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
    this.odahuDataService.updateClasters();
  }
}
