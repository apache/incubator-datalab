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

package com.epam.dlab.configuration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.epam.dlab.exceptions.ParseException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/** Provides schedule time configuration.
 */
public class SchedulerConfiguration {
	
	/** User's schedule. */
	@JsonProperty
	private String schedule = "12, 13:30:23, 18:34, 08:50, 7:80";
	
	
	/** Return the schedule of user.
	 */
	public String getSchedule() {
		return schedule;
	}
	
	/** Set the schedule of user.
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	
	
	/** Schedule. */
	private Map<String, Calendar> realSchedule = new TreeMap<>();
	
	/** Build the schedule from user' schedule.
	 * @throws ParseException
	 */
	public void build() throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		String [] unitArray = schedule.split(",");
		realSchedule.clear();
		for (int i = 0; i < unitArray.length; i++) {
			Calendar date = Calendar.getInstance();
			int [] time = getTime(unitArray[i]);
			try {
				df.parse(StringUtils.join(time, ':'));
			} catch (Exception e) {
				throw new ParseException("Cannot parse date " + unitArray[i] + ". " + e.getLocalizedMessage(), e);
			}
			date.clear();
			date.set(1, 1, 1, time[0], time[1], time[2]);
			realSchedule.put(df.format(date.getTime()), date);
		}
		adjustStartTime();
	}
	
	/** Return the schedule.
	 */
	public Map<String, Calendar> getRealSchedule() {
		return realSchedule;
	}
	
	/** Return time array of user' schedule time.
	 * @param time the time in format HH:mm:ss.
	 * @throws ParseException
	 */
	private int [] getTime(String time) throws ParseException {
		String [] timeString = time.trim().split(":");
		int [] timeInt = new int[3];
		
		for (int i = 0; i < timeInt.length; i++) {
			if (i < timeString.length) {
				try {
					timeInt[i] = Integer.parseInt(timeString[i]);
				} catch (Exception e) {
					throw new ParseException("Cannot parse date " + time + ". " + e.getLocalizedMessage(), e);
				}
			} else {
				timeInt[i] = 0;
			}
		}
		
		return timeInt;
	}

	/** Adjust the time in schedule for current time.
	 */
	public void adjustStartTime() {
		Calendar now = Calendar.getInstance();
		for(String key : realSchedule.keySet()) {
			Calendar time = realSchedule.get(key);
			if (time.before(now)) {
				time.set(now.get(Calendar.YEAR),
						now.get(Calendar.MONTH),
						now.get(Calendar.DAY_OF_MONTH),
						time.get(Calendar.HOUR_OF_DAY),
						time.get(Calendar.MINUTE),
						time.get(Calendar.SECOND));
				if (time.before(now)) {
					time.add(Calendar.DAY_OF_MONTH, 1);
				}
				realSchedule.put(key, time);
			}
		}
	}
	
	/** Return the key of the next start time from the schedule.
	 */
	public String getNextTimeKey() {
		long now = System.currentTimeMillis();
		String nextKey = null;
		long nextTime = -1;
		
		for(String key : realSchedule.keySet()) {
			long time = realSchedule.get(key).getTimeInMillis();
			if ((time >= now && time < nextTime) || nextTime == -1) {
				nextTime = time;
				nextKey = key;
			}
		}
		return nextKey;
	}
	
	/** Return the next start time from the schedule.
	 */
	public Calendar getNextTime() {
		String key = getNextTimeKey();
		return (key == null ? null : realSchedule.get(key));
	}
	
	/** Return the key of the near start time from the schedule to the current time.
	 */
	public String getNearTimeKey() {
		long now = System.currentTimeMillis();
		String nextKey = null;
		long nextTime = -1;
		
		for(String key : realSchedule.keySet()) {
			long time = Math.abs(now - realSchedule.get(key).getTimeInMillis());
			if (time < nextTime || nextTime == -1) {
				nextTime = time;
				nextKey = key;
			}
		}
		return nextKey;
	}
	
	/** Return the near start time from the schedule to the current time.
	 */
	public Calendar getNearTime() {
		String key = getNearTimeKey();
		return (key == null ? null : realSchedule.get(key));
	}
	
	/** Returns a string representation of the object.
	 * @param self the object to generate the string for (typically this), used only for its class name.
	 */
	public ToStringHelper toStringHelper(Object self) {
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		ToStringHelper helper = MoreObjects.toStringHelper(self);
		for(String key : realSchedule.keySet()) {
			Calendar time = realSchedule.get(key);
			helper.add(key, df.format(time.getTime()));
		}
    	return helper;
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this)
    			.toString();
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SchedulerConfiguration)) return false;

		SchedulerConfiguration that = (SchedulerConfiguration) o;

		return getRealSchedule() != null ? getRealSchedule().keySet().equals(that.getRealSchedule().keySet())
				: that.getRealSchedule() == null;
	}

	@Override
	public int hashCode() {
		return getRealSchedule() != null ? getRealSchedule().keySet().hashCode() : 0;
	}
}
