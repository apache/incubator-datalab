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

package com.epam.dlab.module.aws;

import com.epam.dlab.core.FilterBase;
import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.exceptions.ParseException;
import com.epam.dlab.model.aws.ReportLine;
import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Filter and transform the line of AWS detailed billing reports.
 */
@JsonTypeName(ModuleName.FILTER_AWS)
@JsonClassDescription(
		"Amazon Web Services detailed billing reports filter.\n" +
				"Filter report data and select line item only. Set column projectCode and\n" +
				"currencyCode to user values.\n" +
				"  - type: " + ModuleName.FILTER_AWS + "\n" +
				"    [currencyCode: <string>]    - user value for currencyCode column.\n" +
				"    [columnDlabTag: <string>]   - name of column tag of DLab resource id.\n" +
				"    [serviceBaseName: <string>] - DLab's service base name."

)
public class FilterAWS extends FilterBase {

	/**
	 * The code of currency.
	 */
	@NotNull
	@JsonProperty
	private String currencyCode;

	/**
	 * Name of report column tag of DLab.
	 */
	@NotNull
	@JsonProperty
	private String columnDlabTag;

	/**
	 * DLab service base name.
	 */
	@NotNull
	@JsonProperty
	private String serviceBaseName;


	/**
	 * Return the code of currency for billing.
	 */
	public String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Set the code of currency for billing.
	 */
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	/**
	 * Return the name of report column tag of DLab.
	 */
	public String getColumnDlabTag() {
		return columnDlabTag;
	}

	/**
	 * Set the name of report column tag of DLab.
	 */
	public void setDlabTagName(String columnDlabTag) {
		this.columnDlabTag = columnDlabTag;
	}

	/**
	 * Return service base name.
	 */
	public String getServiceBaseName() {
		return serviceBaseName;
	}

	/**
	 * Set service base name.
	 */
	public void setServiceBaseName(String serviceBaseName) {
		this.serviceBaseName = serviceBaseName;
	}


	private int dlabIdIndex = -1;
	private String dlabPrefix;

	@Override
	public void initialize() throws InitializationException {
		dlabIdIndex = (getColumnDlabTag() == null ? -1 :
				getParser().getSourceColumnIndexByName(getColumnDlabTag()));
		dlabPrefix = getServiceBaseName() + ":";
	}

	@Override
	public String canParse(String line) throws ParseException {
		return (line.indexOf("\",\"LineItem\",\"") < 0 ? null : line);
	}

	@Override
	public List<String> canTransform(List<String> row) throws ParseException {
		if (dlabIdIndex != -1 &&
				(row.size() <= dlabIdIndex ||
						!row.get(dlabIdIndex).startsWith(dlabPrefix))) {
			return null;
		}
		return row;
	}

	@Override
	public ReportLine canAccept(ReportLine row) throws ParseException {
		row.setCurrencyCode(currencyCode);
		return row;
	}


	@Override
	public ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("currencyCode", currencyCode)
				.add("columnDlabTag", columnDlabTag)
				.add("serviceBaseName", serviceBaseName);
	}
}
