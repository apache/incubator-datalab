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

package com.epam.dlab.module.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.epam.dlab.core.ModuleData;
import com.epam.dlab.exceptions.AdapterException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Create a file listing of reports from AWS bucket.
 * See details in
 * <a href="http://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/billing-reports.html#detailed-report-with-resources-tags">
 * Detailed billing report with resources and tags</a>.
 */
public class S3FileList {

	/**
	 * Report suffix without date.
	 */
	private static final String REPORT_SUFIX = ".csv";
	/**
	 * Date regex for YYYYMMDD
	 */
	private static final String DATE_REGEX = "\\d{4}(0?[1-9]|1[012])(0?[1-9]|[12][0-9]|3[01])";
	private static final String REGEX = String.format("(^.*/.*/%s-%s)/.*/*.\\%s", DATE_REGEX, DATE_REGEX,
			REPORT_SUFIX);

	/**
	 * Bucket name.
	 */
	private final String bucket;

	/**
	 * Name of last file which is loaded or <b>null</b> for loading all files in bucket folder.
	 */
	private final ModuleData moduleData;
	private final Pattern reportPattern;
	private final boolean awsJobEnabled;


	/**
	 * Instantiate file find class.
	 *
	 * @param awsJobEnabled
	 * @param bucket        the name of bucket.
	 * @param moduleData    data for working module
	 */
	public S3FileList(boolean awsJobEnabled, String bucket, ModuleData moduleData) {
		this.bucket = bucket;
		this.moduleData = moduleData;
		this.awsJobEnabled = awsJobEnabled;
		this.reportPattern = this.awsJobEnabled ? Pattern.compile(REGEX) : Pattern.compile(".*" + REPORT_SUFIX + "$");
	}

	/**
	 * Return the list of files for new reports.
	 *
	 * @param s3Client the S3 client.
	 * @return the list of files.
	 * @throws AdapterException
	 */
	public List<String> getFiles(AmazonS3 s3Client) throws AdapterException {
		final List<S3ObjectSummary> objectSummaries = reportFilesInBillingBucket(s3Client);
		return awsJobEnabled ? lastFilesPerBillingPeriod(objectSummaries) :
				objectSummaries.stream().map(S3ObjectSummary::getKey).sorted().collect(toList());
	}

	private List<S3ObjectSummary> reportFilesInBillingBucket(AmazonS3 s3Client) throws AdapterException {
		ListObjectsV2Request request = new ListObjectsV2Request()
				.withBucketName(bucket);
		ListObjectsV2Result result;
		List<S3ObjectSummary> objectSummaries = new ArrayList<>();
		try {
			do {
				result = s3Client.listObjectsV2(request);
				objectSummaries.addAll(notProcessedFiles(result));
			} while (result.isTruncated());
		} catch (Exception e) {
			throw new AdapterException("Cannot get the file listing of bucket \"" + bucket + "*\". " +
					e.getLocalizedMessage(), e);
		}
		return objectSummaries;
	}

	private List<S3ObjectSummary> notProcessedFiles(ListObjectsV2Result result) {
		return result.getObjectSummaries()
				.stream()
				.filter(this::matchBillingRegexAndWasNotProcessed)
				.collect(toList());
	}

	private boolean matchBillingRegexAndWasNotProcessed(S3ObjectSummary o) {
		return reportPattern.matcher(o.getKey()).matches()
				&& !moduleData.wasProcessed(o.getKey(), o.getLastModified(),
				extractDatePrefix(reportPattern, o));
	}

	/**
	 * Returns list of files that per billing period
	 * For particular billing period file with the biggest modification date will be returned
	 *
	 * @param objectSummaries amazon s3 objects
	 * @return list of file names
	 */
	protected List<String> lastFilesPerBillingPeriod(List<S3ObjectSummary> objectSummaries) {
		final Map<String, List<S3ObjectSummary>> months = objectSummaries.stream()
				.collect(Collectors.groupingBy(o -> extractDatePrefix(reportPattern, o), mapping(o -> o, toList())));

		return months.entrySet()
				.stream()
				.flatMap(this::lastFileForBillingPeriod)
				.sorted()
				.collect(Collectors.toList());
	}

	private Stream<? extends String> lastFileForBillingPeriod(Map.Entry<String, List<S3ObjectSummary>> entry) {
		final List<S3ObjectSummary> assemblyIds = entry.getValue();
		final S3ObjectSummary lastBillingFile = assemblyIds.stream()
				.max(Comparator.comparing(S3ObjectSummary::getLastModified))
				.orElseThrow(() -> new IllegalStateException("AssemblyId does not contains any file"));
		return assemblyIds.stream()
				.filter(s -> s.getKey().startsWith(StringUtils.substringBeforeLast(lastBillingFile.getKey(), "/")))
				.map(S3ObjectSummary::getKey);
	}

	private String extractDatePrefix(Pattern pattern, S3ObjectSummary o) {
		final String key = o.getKey();
		final Matcher matcher = pattern.matcher(key);
		if (matcher.find() && awsJobEnabled) {
			return matcher.group(1);
		} else {
			return key;
		}
	}
}
