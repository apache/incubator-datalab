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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.epam.dlab.core.ModuleData;
import com.epam.dlab.exceptions.AdapterException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Create a file listing of reports from AWS bucket.
 * See details in
 * <a href="http://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/billing-reports.html#detailed-report-with-resources-tags">
 * Detailed billing report with resources and tags</a>.
 */
public class S3FileList {
	/**
	 * Delimiter for AWS object keys.
	 */
	private static final String S3O_DELIMITER = "/";

	/**
	 * Report suffix without date.
	 */
	private static final String REPORT_SUFIX = ".csv.zip";


	/**
	 * Bucket name.
	 */
	private final String bucket;

	/**
	 * Name of last file which is loaded or <b>null</b> for loading all files in bucket folder.
	 */
	private final ModuleData moduleData;


	/**
	 * Instantiate file find class.
	 *
	 * @param bucket     the name of bucket.
	 * @param moduleData data for working module
	 */
	public S3FileList(String bucket, ModuleData moduleData) {
		this.bucket = bucket;
		this.moduleData = moduleData;
	}

	/**
	 * Add new file name to the list.
	 *
	 * @param files    the list of files.
	 * @param filename the name of file.
	 */
	private void addFileToList(List<String> files, String filename) {
		for (int i = files.size() - 1; i >= 0; i--) {
			int compare = filename.compareTo(files.get(i));
			if (compare == 0) {
				return;
			} else if (compare > 0) {
				break;
			}
		}
		files.add(filename);
	}

	/**
	 * Return the list of files for new reports.
	 *
	 * @param s3Client the S3 client.
	 * @return the list of files.
	 * @throws AdapterException
	 */
	public List<String> getFiles(AmazonS3 s3Client) throws AdapterException {
		ListObjectsV2Request request = new ListObjectsV2Request()
				.withBucketName(bucket);
		ListObjectsV2Result result;
		List<String> list = new ArrayList<>();
		try {
			do {
				result = s3Client.listObjectsV2(request);
				for (S3ObjectSummary item : result.getObjectSummaries()) {
					String filename = item.getKey().contains(S3O_DELIMITER) ?
							StringUtils.substringAfterLast(item.getKey(), S3O_DELIMITER) : item.getKey();
					if (filename.endsWith(REPORT_SUFIX) &&
							!moduleData.wasProcessed(item.getKey(), item.getLastModified())) {
						addFileToList(list, item.getKey());

					}
				}
			} while (result.isTruncated());
		} catch (Exception e) {
			throw new AdapterException("Cannot get the file listing of bucket \"" + bucket + "*\". " +
					e.getLocalizedMessage(), e);
		}

		sort(list);
		return list;
	}

	/**
	 * Sort the list of file names.
	 *
	 * @param list the list of file names.
	 */
	protected void sort(List<String> list) {
		list.sort(String::compareTo);
	}
}
