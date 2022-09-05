import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { UserData } from '../image-action.model';

@Component({
  selector: 'datalab-share-user-data',
  templateUrl: './share-user-data.component.html',
  styleUrls: ['./share-user-data.component.scss'],
})
export class ShareUserDataComponent implements OnInit {
  @Input() userData: UserData;

  @Output() removeUserData: EventEmitter<string> = new EventEmitter<string>();

  constructor() { }

  ngOnInit(): void {
  }

  removeUser(userData: string): void {
    this.removeUserData.emit(userData);
  }
}
