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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.epam.dlab.exception.AdapterException;

/** Create a file listing of reports from AWS bucket.
 * Format of the report file name: {@code <AWS account number>-aws-billing-detailed-line-items-with-resources-and-tags-yyyy-mm.csv.zip} 
 * <br>
 * See details in
 * <a href="http://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/billing-reports.html#detailed-report-with-resources-tags">
 * Detailed billing report with resources and tags</a>.
*/
public class S3FileList {
	/** Delimiter for AWS object keys. */
	private static final char S3O_DELIMITER = '/';
	
	/** Pattern of date format YYYY-MM. */
	private static final Pattern PATTERN_DATE = Pattern.compile("^20[0-9]{2}-[0-1][0-9]$");
	
	/** Report prefix without account id. */
	private static final String REPORT_PREFIX = "-aws-billing-detailed-line-items-with-resources-and-tags-";

	/** Report suffix without date. */
	private static final String REPORT_SUFIX = ".csv.zip";

	
	/** Bucket name. */
	private final String bucket;
	
	/** Report prefix without date. */
	private final String reportPrefix;
	
	/** Name of last file which is loaded or <b>null</b> for loading all files in bucket folder. */
	private final String lastFilename;
	
	/** Date of last file modification. */
	private final Date lastModificationDate;
	
	/** Instantiate file find class.
	 * @param bucket the name of bucket.
	 * @param path the path to report.
	 * @param accountId the AWS account number.
	 * @param lastFilename the name of file which is loaded last time or <b>null</b> for loading all report files in the bucket folder.
	 * @param lastModificationDate the modification date of file which is loaded last time or <b>null</b> if it is unknown.
	 */
	public S3FileList(String bucket, String path, String accountId, String lastFilename, Date lastModificationDate) {
		this.bucket = bucket;
		this.reportPrefix = (path == null ? "" : path + S3O_DELIMITER) + accountId + REPORT_PREFIX;
		this.lastFilename = lastFilename;
		this.lastModificationDate = lastModificationDate;
	}
	
	/** Add new file name to the list.
	 * @param files the list of files.
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

	/** Return the list of files for new reports.
	 * @param s3Client the S3 client.
	 * @return the list of files.
	 * @throws AdapterException
	 */
	public List<String> getFiles(AmazonS3 s3Client) throws AdapterException {
		ListObjectsV2Request request = new ListObjectsV2Request()
				.withBucketName(bucket)
				.withPrefix(reportPrefix)
				;
		ListObjectsV2Result result;
		List<String> list = new ArrayList<>();
		try {
			do {
				result = s3Client.listObjectsV2(request);
				for(S3ObjectSummary item : result.getObjectSummaries()) {
					String filename = item.getKey();
					if (filename.endsWith(REPORT_SUFIX)) {
						String dateName = filename.substring(reportPrefix.length(), filename.length() - REPORT_SUFIX.length());
						if (PATTERN_DATE.matcher(dateName).matches()) {
							int compare = (lastFilename == null ? 1 : filename.compareTo(lastFilename));
							if (compare > 0) {
								addFileToList(list, filename);
							} else if (compare == 0 &&
										(lastModificationDate == null || item.getLastModified().compareTo(lastModificationDate) > 0)) {
								addFileToList(list, filename);
							}
						}
					}
				}
			} while (result.isTruncated());
		} catch (Exception e) {
			throw new AdapterException("Cannot get the file listing of bucket \"" + bucket + S3O_DELIMITER + reportPrefix + "*\". " +
					e.getLocalizedMessage(), e);
		}
		
		sort(list);
		return list;
	}
	
	/** Sort the list of file names.
	 * @param list the list of file names.
	 */
	protected void sort(List<String> list) {
		list.sort(new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		});
	}
}
