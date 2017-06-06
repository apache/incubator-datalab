import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'dlab-reporting-grid',
  templateUrl: './reporting-grid.component.html',
  styleUrls: ['./reporting-grid.component.css',
              '../../resources/resources-grid/resources-grid.component.css']
})
export class ReportingGridComponent implements OnInit {

  constructor() { }

  public filteringColumns: Array<any> = [
    { title: 'User', name: 'user', className: 'th_name', filtering: {}, role: 'admin'},
    { title: 'Environment name', name: 'name', className: 'th_name', filtering: {} },
    { title: 'Resource Type', name: 'type', className: 'th_type', filtering: {} },
    { title: 'Environment shape', name: 'shape', className: 'th_shape', filtering: {} },
    { title: 'Service', name: 'service', className: 'th_service', filtering: {} },
    { title: 'Service Charges', name: 'charges', className: 'th_charges' },
    { title: 'Cloud provider', className: 'th_provider' }
  ];

  ngOnInit() {}

}
