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

package com.epam.dlab.module;

import com.epam.dlab.core.AdapterBase;
import com.epam.dlab.core.parser.CommonFormat;
import com.epam.dlab.exceptions.AdapterException;
import com.epam.dlab.model.aws.ReportLine;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

/**
 * The adapter for console output.
 */
@JsonTypeName(ModuleName.ADAPTER_CONSOLE)
@JsonClassDescription(
		"Console adapter.\n" +
				"Output data to console. Can be used for AdapterOut only.\n" +
				"  - type: " + ModuleName.ADAPTER_CONSOLE + "\n" +
				"    [writeHeader: <true | false>]  - write header of data to the adapterOut."
)
public class AdapterConsole extends AdapterBase {

	/**
	 * Default constructor for deserialization.
	 */
	public AdapterConsole() {
	}

	/**
	 * Instantiate adapter for reading or writing.
	 *
	 * @param mode the mode of adapter.
	 */
	public AdapterConsole(Mode mode) {
		super(mode);
	}


	@Override
	public void open() throws AdapterException {
		if (getMode() != Mode.WRITE) {
			throw new AdapterException("Mode of " + getType() + " adapter may be " + Mode.WRITE + " only.");
		}
	}

	@Override
	public void close() throws AdapterException {
		// Nothing to do
	}

	@Override
	public String getEntryName() {
		return "console";
	}

	@Override
	public String readLine() throws AdapterException {
		throw new AdapterException("Unimplemented method called.");
	}

	@Override
	public void writeHeader(List<String> header) throws AdapterException {
		System.out.println(CommonFormat.rowToString(header));
	}

	@Override
	public void writeRow(ReportLine row) throws AdapterException {
		System.out.println(CommonFormat.rowToString(row));
	}
}
