import { Injectable } from '@angular/core';
import { ProjectModel } from './images.model';

@Injectable({
  providedIn: 'root'
})
export class ImagesService {
  projectList!: ProjectModel[];

  constructor() { }
}
