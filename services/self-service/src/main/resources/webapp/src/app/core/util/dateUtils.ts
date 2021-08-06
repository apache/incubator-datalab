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

export class DateUtils {
  public static diffBetweenDatesInHours(dateString: number): number {
    const currentDate = new Date();
    const creationDate = new Date(dateString);

    let hourDifference = 0;
    hourDifference = Math.round(Math.abs(currentDate.getTime() - creationDate.getTime()) / 36e5);

    return hourDifference;
  }

  public getQuarterRange() {
    const currentMonth = new Date().getMonth();
    const yyyy = new Date().getFullYear();
    const start = (Math.floor(currentMonth / 3) * 3 ) + 1,
          end = start + 3,
          startDate = new Date(start + '-01-' + yyyy);
    let endDate = end > 12 ? new Date('01-01-' + (yyyy + 1)) : new Date(end + '-01-' + (yyyy));

    endDate = new Date((endDate.getTime()) - 1);
    console.log('startDate =', startDate, 'endDate =', endDate);
  }
}
