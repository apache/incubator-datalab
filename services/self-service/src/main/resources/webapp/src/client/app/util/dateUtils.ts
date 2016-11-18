/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

export class DateUtils {
  public static diffBetweenDatesInHours(dateString : number) : number {
    let currentDate = new Date();
    let creationDate = new Date(dateString);

    let hourDifference = 0;
    hourDifference = Math.round(Math.abs(currentDate.getTime() - creationDate.getTime()) / 36e5);

    return hourDifference;
  }
}
