import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OdahuActionDialogComponent } from './odahu-action-dialog.component';
import { MaterialModule } from '../../material.module';
import {FormsModule} from '@angular/forms';

export * from './odahu-action-dialog.component';

@NgModule({
  imports: [CommonModule, MaterialModule, FormsModule],
  declarations: [OdahuActionDialogComponent],
  entryComponents: [OdahuActionDialogComponent],
  exports: [OdahuActionDialogComponent]
})
export class OdahuActionDialogModule {}
