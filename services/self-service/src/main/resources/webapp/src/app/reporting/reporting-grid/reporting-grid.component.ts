import { Component, OnInit } from '@angular/core';
import { BillingReportService }  from '../../core/services';

@Component({
  selector: 'dlab-reporting-grid',
  templateUrl: './reporting-grid.component.html',
  styleUrls: ['./reporting-grid.component.css',
              '../../resources/resources-grid/resources-grid.component.css']
})
export class ReportingGridComponent implements OnInit {

  collapseFilterRow: boolean = false;

  constructor(private billingReportService: BillingReportService) { }

  public filteringColumns: Array<any> = [
    { title: 'User', name: 'user', className: 'th_user', filtering: {}, role: 'admin'},
    { title: 'Environment name', name: 'name', className: 'th_env_name', filtering: {} },
    { title: 'Resource Type', name: 'type', className: 'th_type', filtering: {} },
    { title: 'Shape', name: 'shape', className: 'th_shape', filtering: {} },
    { title: 'Service', name: 'service', className: 'th_service', filtering: {} },
    { title: 'Service Charges', name: 'charges', className: 'th_charges' },
    // { title: 'Cloud provider', className: 'th_provider' },
    { title: 'Actions', className: 'th_actions' }

  ];

  ngOnInit() {
    this.getGeneralBillingData();
  }

  onUpdate($event) {
    console.log($event);
  }

  toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  getGeneralBillingData() {
    this.billingReportService.getGeneralBillingData()
      .subscribe(data => console.log(data)
    );
  }
}
