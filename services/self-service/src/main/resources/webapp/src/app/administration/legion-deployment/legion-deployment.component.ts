import { Component, OnInit } from '@angular/core';
import {LegionDeploymentDataService} from "./legion-deployment-data.service";
import {Subscription} from "rxjs";
import {MatDialog} from "@angular/material/dialog";
import {ToastrService} from "ngx-toastr";
import {CreateLegionClusterComponent} from "./create-legion-claster/create-legion-cluster.component";
import {HealthStatusService, LegionDeploymentService} from "../../core/services";

export interface OdahuCluster {
  project: string;
  endpoint: string;
  name: string;
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
  }

  public createLegionClaster(): void {
    this.dialog.open(CreateLegionClusterComponent, { panelClass: 'modal-lg' })
      .afterClosed().subscribe((result: OdahuCluster[]) => {
      result && this.createLegionCluster(result);
        this.refreshGrid()
      });
  }

  private createLegionCluster(value): void{
    this.legionDeploymentService.addLegionCluster(value);
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => this.healthStatus = result);
  }

  private refreshGrid(): void {
    this.legionDeploymentDataService.updateClasters();
  }
}
