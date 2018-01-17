import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'time',
  templateUrl: './time-picker.component.html',
  styleUrls: ['./time-picker.component.scss']
})
export class TimePickerComponent implements OnInit {

  constructor() { }

  ngOnInit() {
    console.log('TIME PICK');
  }

}
