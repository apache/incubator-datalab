/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Component, OnInit, ElementRef, ViewEncapsulation, ViewContainerRef, ViewChild, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import Guacamole from 'guacamole-common-js';

import { environment } from '../../environments/environment';

// we can now access environment.apiUrl
const API_URL = environment.apiUrl;

import { StorageService, HealthStatusService } from '../core/services';
import { FileUtils } from '../core/util';

@Component({
  selector: 'dlab-webterminal',
  templateUrl: './webterminal.component.html',
  styleUrls: ['./webterminal.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WebterminalComponent implements OnInit {
  public id: string;
  public endpoint: string;
  public state: string = '';
  public status: any;
  public guacamole: any;
  public tunnel: any;


  @ViewChild('terminal', { read: ElementRef, static: false }) terminal: ElementRef;
  @ViewChild('clip', { static: true }) clip;


  constructor(
    private route: ActivatedRoute,
    private storageService: StorageService,
    private healthStatusService: HealthStatusService,
    @Inject(DOCUMENT) private document) {
  }

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id');
    this.endpoint = this.route.snapshot.paramMap.get('endpoint');
    this.openTerminal();
  }

  public openTerminal() {

    this.guacamoleTunnel();
    this.guacamoleConnect();

    this.tunnel.onerror = (error) => {
      if (error.message = 'Unauthorized') {
        this.getStatus();
      }
    };

    this.guacamole.onstatechange = (state) => {
      console.log(state);

      const TUNNEL_STATE_MAP = {
        STATE_IDLE: 0,
        STATE_CONNECTING: 1,
        STATE_WAITING: 2,
        STATE_CONNECTED: 3,
        STATE_DISCONNECTING: 4,
        STATE_DISCONNECTED: 5
      }
    };

    window.onunload = () => this.guacamole.disconnect();

    const mouse = new Guacamole.Mouse(this.guacamole.getDisplay().getElement());
    mouse.onmousemove = (mouseState) => {
      this.guacamole.sendMouseState(mouseState);
      console.log(this.tunnel.state);
    }

    const keyboard = new Guacamole.Keyboard(document);
    keyboard.onkeydown = (keysym) => this.guacamole.sendKeyEvent(1, keysym);
    keyboard.onkeyup = (keysym) => this.guacamole.sendKeyEvent(0, keysym);
  }

  private guacamoleConnect() {
    this.guacamole.connect(`{"host" : "${this.id}", "endpoint" : "${this.endpoint}"}`);
  }

  private guacamoleTunnel() {
    const url = environment.production ? window.location.origin : API_URL;
    this.tunnel = new Guacamole.HTTPTunnel(`${url}/api/tunnel`, true, { 'Authorization': `Bearer ${this.storageService.getToken()}` });
    this.guacamole = new Guacamole.Client(this.tunnel);

    const display = document.getElementById('display');
    display.appendChild(this.guacamole.getDisplay().getElement());
  }

  private getStatus() {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
      result => {
        this.status = result || null;

        this.guacamoleTunnel();
        this.guacamoleConnect();
      },
      error => console.log('Status loading failed!'));
  }
}