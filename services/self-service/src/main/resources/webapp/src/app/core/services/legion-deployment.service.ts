import { Injectable } from '@angular/core';
import {from} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class LegionDeploymentService {
  public list =  [{clasters: [
      // {
      //   project: "Project5",
      //   endpoint: "Endpoint24",
      //   name: "claster1",
      // },
      // {
      //   project: "Project4",
      //   endpoint: "Endpoint23",
      //   name: "claster1",
      // },
      // {
      //   project: "Project3",
      //   endpoint: "Endpoint21",
      //   name: "claster1",
      // }
      ]}];
  constructor() { }

  public getLegionClasters(){
    const obsList = from(this.list);
    return obsList
  }

  public addLegionCluster(cluster) {
    this.list[0].clasters.push(cluster);
  }
}
