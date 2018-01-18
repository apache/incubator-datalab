import { Component, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormControl, FormArray, FormBuilder } from '@angular/forms';

@Component({
  selector: 'dlab-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.css']
})
export class SchedulerComponent implements OnInit {

  public notebook: any;
  private exportTime = {
    hour: new Date().getHours(),
    minute: new Date().getMinutes(),
    meriden: new Date().getHours() < 12 ? 'AM' : 'PM'
  };

  public weekdays: string[] = [ 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday' ];
  schedulerFormGroup : FormGroup;

  @ViewChild('bindDialog') bindDialog;

  constructor(private formBuilder: FormBuilder) { }

  ngOnInit() {
    this.schedulerFormGroup = this.formBuilder.group({
      weekdays: this.formBuilder.array([])
    });
  }

  public open(param, notebook): void {
    this.notebook = notebook;

    this.bindDialog.open(param);
  }

  onChange(event) {
    console.log(event);

    const weekdays = <FormArray>this.schedulerFormGroup.get('weekdays') as FormArray;

    if (event.checked) {
      weekdays.push(new FormControl(event.source.name))
    } else {
      const i = weekdays.controls.findIndex(x => x.value === event.source.name);
      weekdays.removeAt(i);
    }
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }
}
