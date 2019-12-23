import { Injectable } from '@angular/core';
import {Subject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ProgressBarService {
  public showProgressBar = new Subject();

  constructor() { }

  public toggleProgressBar(value) {
    this.showProgressBar.next(value);
  }

  public stopProgressBar() {
    this.showProgressBar.next(false);
  }

  public startProgressBar() {
    this.showProgressBar.next(true);
  }
}
