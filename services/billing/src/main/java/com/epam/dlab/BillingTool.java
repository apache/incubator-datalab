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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.configuration.BillingToolConfiguration;
import com.epam.dlab.configuration.BillingToolConfigurationFactory;
import com.epam.dlab.core.parser.ParserBase;
import com.epam.dlab.exception.AdapterException;
import com.epam.dlab.exception.InitializationException;
import com.epam.dlab.exception.ParseException;
import com.fasterxml.jackson.databind.JsonNode;

/** Provides billing parser features.
 */
public class BillingTool {
	private static final Logger LOGGER = LoggerFactory.getLogger(BillingTool.class);
	
	/** Runs parser for given configuration.
	 * @param conf billing configuration.
	 * @throws InitializationException
	 * @throws AdapterException
	 * @throws ParseException
	 */
	public void run(BillingToolConfiguration conf) throws InitializationException, AdapterException, ParseException {
		ParserBase parser = conf.build();
		LOGGER.debug("Billing Tool Configuration: {}", conf);
		LOGGER.debug("Parser configuration: {}", parser);
		
		parser.parse();
		LOGGER.debug("Billing Tool statistics: {}", parser.getStatistics());
	}
	
	/** Runs parser for given configuration in file.
	 * @param filename the name of file for billing configuration.
	 * @throws InitializationException
	 * @throws AdapterException
	 * @throws ParseException
	 */
	public void run(String filename) throws InitializationException, AdapterException, ParseException {
		run(BillingToolConfigurationFactory.build(filename, BillingToolConfiguration.class));
	}
	
	/** Runs parser for given configuration.
	 * @param jsonNode the billing configuration.
	 * @throws InitializationException
	 * @throws AdapterException
	 * @throws ParseException
	 */
	public void run(JsonNode jsonNode) throws InitializationException, AdapterException, ParseException {
		run(BillingToolConfigurationFactory.build(jsonNode, BillingToolConfiguration.class));
	}
	
	
	
	/** Check the key name for command line.
	 * @param keyName the name of key.
	 * @param arg the argument from command line.
	 * @return <b>true</b> if given argument is key.
	 */
	private static boolean isKey(String keyName, String arg) {
		return (("--" + keyName).equalsIgnoreCase(arg) ||
				("/" + keyName).equalsIgnoreCase(arg));
	}
	
	/** Sets system error code and throws exception.
	 * @param exitCode error code.
	 * @param message the error message.
	 * @throws InitializationException
	 */
	private static void exitWithError(int exitCode, String message) throws InitializationException {
		System.exit(exitCode);
		throw new InitializationException(message);
	}
	
	/** Runs parser for given configuration.
	 * @param args the arguments of command line. 
	 * @throws InitializationException
	 */
	public static void main(String[] args) throws InitializationException {
		String confName = null;
		String json = null;
		
		for(int i = 0; i < args.length; i++) {
			if (isKey("help", args[i])) {
				i++;
				Help.usage(i < args.length ? Arrays.copyOfRange(args, i, args.length) : null);
				System.exit(0);
				return;
			} else if (isKey("conf", args[i])) {
				i++;
				if (i < args.length) {
					confName = args[i];
				} else {
					exitWithError(2, "Missing the name of configuration file");
				}
			} else if (isKey("json", args[i])) {
				i++;
				if (i < args.length) {
					json = args[i];
				} else {
					exitWithError(2, "Missing the content of json configuration");
				}
			} else {
				exitWithError(2, "Unknow argument: " + args[i]);
			}
		}
		
		if (confName == null && json == null) {
			Help.usage();
			exitWithError(2, "Missing arguments");
		}
		
		if (confName != null && json != null) {
			Help.usage();
			exitWithError(2, "Invalid arguments.");
		}

		try {
			if (confName != null) {
				new BillingTool().run(confName);
			} else {
				new BillingTool().run(json);
			}
		} catch (Exception e) {
			System.exit(1);
			e.printStackTrace();
		}
	}
}
