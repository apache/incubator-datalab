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

package com.epam.dlab;

import com.epam.dlab.configuration.BillingToolConfiguration;
import com.epam.dlab.configuration.BillingToolConfigurationFactory;
import com.epam.dlab.configuration.SchedulerConfiguration;
import com.epam.dlab.core.parser.ParserBase;
import com.epam.dlab.exceptions.AdapterException;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.exceptions.ParseException;
import com.epam.dlab.util.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Billing scheduler for loading billing report.
 */
public class BillingScheduler implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(BillingScheduler.class);

	/**
	 * Timeout for check the schedule in milliseconds.
	 */
	private static final long CHECK_TIMEOUT_MILLIS = 60000;

	/**
	 * Billing scheduler instance.
	 */
	private static BillingScheduler scheduler;
	private final boolean enabled;

	/**
	 * Starts the scheduler for given configuration.
	 *
	 * @param filename the name of file for billing configuration.
	 * @throws InitializationException
	 */
	public static void start(String filename) throws InitializationException {
		if (scheduler == null) {
			scheduler = new BillingScheduler(filename);
			scheduler.thread.start();
		} else {
			LOGGER.debug("Billing scheduler already started");
		}
	}

	/**
	 * Stops the scheduler.
	 */
	public static void stop() {
		if (scheduler.thread != null) {
			LOGGER.debug("Billing scheduler will be stopped ...");
			synchronized (scheduler.thread) {
				scheduler.thread.interrupt();
				scheduler.thread = null;
			}
			LOGGER.info("Scheduler has been stopped");
		}
	}


	/**
	 * Thread of the scheduler.
	 */
	private Thread thread = new Thread(this, this.getClass().getSimpleName());

	/**
	 * Name of configuration file.
	 */
	private final String confFilename;

	/**
	 * Current schedule.
	 */
	private SchedulerConfiguration schedule;

	/**
	 * Instantiate billing scheduler for given configuration.
	 *
	 * @param filename the name of file for billing configuration.
	 * @throws InitializationException
	 */
	public BillingScheduler(String filename) throws InitializationException {
		this.confFilename = filename;
		// Check configuration
		LOGGER.debug("Billing report configuration file: {}", filename);
		BillingToolConfiguration configuration = BillingToolConfigurationFactory.build(confFilename,
				BillingToolConfiguration.class);
		@SuppressWarnings("unused")
		ParserBase parser = configuration.build();
		this.enabled = configuration.isBillingEnabled();
		LOGGER.debug("Billing report configuration: {}", configuration);
		setSchedule(configuration);
	}

	/**
	 * Loads the billing report.
	 *
	 * @throws InitializationException
	 * @throws AdapterException
	 * @throws ParseException
	 */
	private void load() throws InitializationException, AdapterException, ParseException {
		BillingToolConfiguration configuration = BillingToolConfigurationFactory.build(confFilename,
				BillingToolConfiguration.class);
		ParserBase parser = configuration.build();
		long time = schedule.getNearTime().getTimeInMillis();
		if (setSchedule(configuration)) {
			if (time != schedule.getNearTime().getTimeInMillis()) {
				LOGGER.info("Previous billing schedule has been canceled");
				return;
			}
		}

		LOGGER.info("Try to laod billing report for configuration: {}", configuration);
		parser.parse();
		if (parser.getStatistics().size() > 0) {
			LOGGER.info("Billing report parser statistics:");
			for (int i = 0; i < parser.getStatistics().size(); i++) {
				LOGGER.info("  {}", parser.getStatistics().get(i).toString());
			}
		}
	}

	/**
	 * Read the schedule from configuration.
	 *
	 * @param configuration the billing configuration.
	 * @return <b>true>/b> if new schedule was loaded, otherwise <b>false</b>.
	 * @throws InitializationException
	 */
	private boolean setSchedule(BillingToolConfiguration configuration) throws InitializationException {
		SchedulerConfiguration schedule = configuration.getScheduler();
		boolean isModified = false;
		if (schedule == null) {
			throw new InitializationException("Schedule of billing report in configuration file \"" + confFilename + "" +
					" not found");
		}
		if (this.schedule == null) {
			isModified = true;
			LOGGER.debug("Billing report schedule: {}", schedule);
		} else {
			this.schedule.adjustStartTime();
			if (!schedule.equals(this.schedule)) {
				isModified = true;
				LOGGER.debug("New billing report schedule has been loaded: {}", schedule);
			}
		}

		try {
			this.schedule = new SchedulerConfiguration();
			this.schedule.setSchedule(schedule.getSchedule());
			this.schedule.build();
		} catch (Exception e) {
			throw new InitializationException("Cannot configure billing scheduler. " + e.getLocalizedMessage(), e);
		}

		return isModified;
	}

	@Override
	public void run() {
		if (enabled) {
			LOGGER.info("Billing scheduler has been started");
			long startTimeMillis = schedule.getNextTime().getTimeInMillis();
			long timeMillis;
			LOGGER.info("Billing report will be loaded at {}", schedule.getNextTime().getTime());

			try {
				while (!Thread.currentThread().isInterrupted()) {
					if (startTimeMillis <= System.currentTimeMillis()) {
						try {
							LOGGER.debug("Try to load billing report for schedule {}", schedule.getNextTime().getTime
									());
							load();
						} catch (InitializationException | AdapterException | ParseException e) {
							LOGGER.error("Error loading billing report: {}", e.getLocalizedMessage(), e);
						}
						startTimeMillis = schedule.getNextTime().getTimeInMillis();
						LOGGER.info("Billing report will be loaded at {}", schedule.getNextTime().getTime());
					} else {
						schedule.adjustStartTime();
						timeMillis = schedule.getNextTime().getTimeInMillis();
						if (startTimeMillis != timeMillis) {
							LOGGER.info("Billing report will be loaded at {}", schedule.getNextTime().getTime());
							startTimeMillis = timeMillis;
						}
					}

					try {
						timeMillis = startTimeMillis - System.currentTimeMillis();
						if (timeMillis > 0) {
							timeMillis = Math.min(CHECK_TIMEOUT_MILLIS, timeMillis);
							Thread.sleep(timeMillis);
						}
					} catch (InterruptedException e) {
						LOGGER.warn("Billing scheduler interrupted", e);
						Thread.currentThread().interrupt();
					}
				}
			} catch (Exception e) {
				LOGGER.error("Unhandled billing report error: {}", e.getLocalizedMessage(), e);
			}
			LOGGER.info("Scheduler has been stopped");
		} else {
			LOGGER.info("Billing scheduler is disabled");
		}
	}


	/**
	 * Runs billing scheduler for given configuration file.
	 *
	 * @param args the arguments of command line.
	 * @throws InitializationException
	 */
	public static void main(String[] args) throws InitializationException {
		if (ServiceUtils.printAppVersion(BillingTool.class, args)) {
			return;
		}

		String confName = null;
		for (int i = 0; i < args.length; i++) {
			if (BillingTool.isKey("help", args[i])) {
				i++;
				Help.usage(i < args.length ? Arrays.copyOfRange(args, i, args.length) : null);
				return;
			} else if (BillingTool.isKey("conf", args[i])) {
				i++;
				if (i < args.length) {
					confName = args[i];
				} else {
					throw new InitializationException("Missing the name of configuration file");
				}
			} else {
				throw new InitializationException("Unknow argument: " + args[i]);
			}
		}

		if (confName == null) {
			Help.usage();
			throw new InitializationException("Missing arguments");
		}

		BillingTool.setLoggerLevel();
		try {
			start(confName);
		} catch (Exception e) {
			throw new DlabException("Billing scheduler failed", e);
		}
	}
}
