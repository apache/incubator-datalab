import { Component, OnInit } from '@angular/core';
import {Subscription} from "rxjs";
import {LegionDeploymentDataService} from "../legion-deployment-data.service";
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'legion-list',
  templateUrl: './legion-list.component.html',
  styleUrls: ['./legion-list.component.scss']
})
export class LegionListComponent implements OnInit {
  private legionClastersList: any[];
  private subscriptions: Subscription = new Subscription();
  private dataSource: MatTableDataSource<unknown>;
  displayedColumns: string[] = ['name', 'endpoint-url', 'legion-name', 'legion-status', "actions"];

  constructor(
    private legionDeploymentDataService: LegionDeploymentDataService,
  ) { }

  ngOnInit() {
    this.subscriptions.add(this.legionDeploymentDataService._legionClasters.subscribe(
      (value) => {
        if (value) {
          this.legionClastersList = value;
          this.dataSource = new MatTableDataSource(value);
        }
      }));
  }

}
