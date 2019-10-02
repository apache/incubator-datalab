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

import { Component, OnInit, ViewEncapsulation, ViewContainerRef, ViewChild, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import Guacamole from 'guacamole-common-js';

import { environment } from '../../environments/environment';

// we can now access environment.apiUrl
const API_URL = environment.apiUrl;

import { StorageService } from '../core/services';

@Component({
  selector: 'dlab-webterminal',
  templateUrl: './webterminal.component.html',
  styleUrls: ['./webterminal.component.scss']
})
export class WebterminalComponent implements OnInit {
  public id: string;
  public endpoint: string;
  public state: string = '';
  @ViewChild('terminal', { read: ViewContainerRef, static: false }) terminal: ViewContainerRef;
  @ViewChild('clip', { static: true }) clip;


  constructor(
    private route: ActivatedRoute,
    private storageService: StorageService,
    @Inject(DOCUMENT) private document) {
  }

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id');
    console.log(this.id);
    this.open(this.id);
  }

  // public open(id_parameter: string) {
  //   // added to simplify development process
  //   const url = environment.production ? window.location.origin : API_URL;
  //   const tunnel = new Guacamole.HTTPTunnel(
  //     `${url}/api/tunnel`, false,
  //     { 'Authorization': `Bearer ${this.storageService.getToken()}` }
  //   );

  //   const guac = new Guacamole.Client(tunnel);
  //   const display = document.getElementById('display');

  //   display.appendChild(guac.getDisplay().getElement());
  //   const guacDisplay = guac.getDisplay();
  //   const layer = guacDisplay.getDefaultLayer();

  //   guac.connect(id_parameter);

  //   // Error handler
  //   guac.onerror = (error) => console.log(error.message);
  //   window.onunload = () => guac.disconnect();

  //   // Mouse
  //   const mouse = new Guacamole.Mouse(guac.getDisplay().getElement());
  //   mouse.onmousemove = (mouseState) => guac.sendMouseState(mouseState);

  //   const keyboard = new Guacamole.Keyboard(document);
  //   keyboard.onkeydown = (keysym) => guac.sendKeyEvent(1, keysym);
  //   keyboard.onkeyup = (keysym) => guac.sendKeyEvent(0, keysym);
  // }

  public open(id_parameter: string) {
    // added to simplify development process
    const url = environment.production ? window.location.origin : API_URL;
    const tunnel = new Guacamole.HTTPTunnel(
      `${url}/api/tunnel`, false,
      { 'Authorization': `Bearer ${this.storageService.getToken()}` }
    );

    var display = document.getElementById('display');
    var menu = document.getElementById('menu');
    var clipboardElement = document.getElementById('clipboard');

    var guac = new Guacamole.Client(tunnel);

    display.appendChild(guac.getDisplay().getElement());
    // const guacDisplay = guac.getDisplay();

    const keyboard = new Guacamole.Keyboard(document);
    keyboard.onkeydown = (keysym) => guac.sendKeyEvent(1, keysym);
    keyboard.onkeyup = (keysym) => guac.sendKeyEvent(0, keysym);

    const mouse = new Guacamole.Mouse(guac.getDisplay().getElement());
    mouse.onmousedown =
      mouse.onmouseup =
      mouse.onmousemove = function (mouseState) {
        guac.sendMouseState(mouseState);
      };


    guac.onclipboard = (stream, mimetype) => {
      var f;
      if (/^text\//.exec(mimetype)) {
        f = new Guacamole.StringReader(stream);
        var e = "";
        f.ontext = (a) => {
          e += a
        };
        f.onend = () => {
          debugger
          this.clip.nativeElement.value = decodeUnicode(e);
        }
      }
    }

    function decodeUnicode(str) {
      str = str.replace(/\\/g, "%");
      return unescape(str);
    }

    guac.connect(id_parameter);
  }



}
