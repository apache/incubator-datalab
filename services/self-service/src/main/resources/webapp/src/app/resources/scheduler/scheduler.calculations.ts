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

export class SchedulerCalculations {
  public static convertTimeFormat(time24: any) {
    let result;
    if (typeof time24 === 'string') {
      const spl = time24.split(':');

      result = {
        hour: +spl[0] % 12 || 12,
        minute: +spl[1],
        meridiem: +spl[0] < 12 || +spl[0] === 24 ? 'AM' : 'PM'
      };
    } else {
      let hours = time24.hour;
      const minutes = (time24.minute < 10) ? '0' + time24.minute : time24.minute;

      if (time24.meridiem === 'PM' && time24.hour < 12) hours = time24.hour + 12;
      if (time24.meridiem === 'AM' &&  time24.hour === 12) hours = time24.hour - 12;
      hours = hours < 10 ? '0' + hours : hours;

      result = `${hours}:${minutes}`;
    }
    return result;
  }

  public static setTimeInMiliseconds(timeObj) {
    let time = {...timeObj};

    const minutes = (Number(time.minute) < 10) ? ('0' + time.minute) : time.minute;
    const timeMilisec = new Date().setMinutes(+minutes);

    if (time.meridiem === 'PM' && time.hour < 12) time.hour += 12;
    if (time.meridiem === 'AM' && time.hour === 12) time.hour -= 12;

    return new Date(timeMilisec).setHours(time.hour);
  }
}
