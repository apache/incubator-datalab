import { Component, OnInit, ViewChild } from '@angular/core';

@Component({
  selector: 'dlab-manage-ungit',
  templateUrl: './manage-ungit.component.html',
  styleUrls: ['./manage-ungit.component.css',
              '../exploratory/install-libraries/install-libraries.component.css']
})
export class ManageUngitComponent implements OnInit {

   @ViewChild('bindDialog') bindDialog;
  constructor() { }

  ngOnInit() {
  }

  public open(param): void {
    if (!this.bindDialog.isOpened)
      this.bindDialog.open(param);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }
}
