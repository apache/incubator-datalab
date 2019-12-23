import { Component, OnInit } from '@angular/core';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {ProgressBarService} from "../../core/services/progress-bar.service";

@Component({
  selector: 'dlab-progress-bar',
  templateUrl: './progress-bar.component.html',
  styleUrls: ['./progress-bar.component.scss']
})
export class ProgressBarComponent implements OnInit {
  constructor(
    public progressBarService: ProgressBarService,
  ) { }

  ngOnInit() {

  }
}
