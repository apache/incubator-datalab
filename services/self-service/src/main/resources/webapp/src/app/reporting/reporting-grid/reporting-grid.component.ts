import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'dlab-reporting-grid',
  templateUrl: './reporting-grid.component.html',
  styleUrls: ['./reporting-grid.component.css',
              '../../resources/resources-grid/resources-grid.component.css']
})
export class ReportingGridComponent implements OnInit {

  collapseFilterRow: boolean = false;

  constructor() { }

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

  ngOnInit() {}

  onUpdate($event) {
    console.log($event);
  }

  toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }
}
