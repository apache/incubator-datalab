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
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.epam.dlab.exception.AdapterException;

/** Create a file listing of reports from AWS bucket.
 * The AWS Cost and Usage report S3 organization and naming conventions: 
 * {@code <report-prefix>/<report-name>/yyyymmdd-yyyymmdd/<assemblyId>/<report-name>-<file-number>.csv.<zip|gz>}
 * <br>
 * See details in
 * <a href="http://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/billing-reports.html#enhanced-organization">
 * AWS Cost and Usage Report Files</a>.
*/
public class S3FileList {
	/** Delimiter for AWS object keys.*/
	private static final char S3O_DELIMITER = '/';
	
	/** Pattern of date range.*/
	private static final String PATTERN_DATE = "20[0-9]{2}[0-1][0-9][0-3][0-9]";
	
	/** Bucket name.*/
	private final String bucket;
	
	/** Prefix assigned to the report when you created the report in AWS. */
	private final String reportPrefix;
	
	/** Name of report assigned to the report when you created the report in AWS. */
	private final String reportName;
	
	/** ID that AWS creates each time that the report is updated. */
	private final String assemblyId;
	
	/** Name of last file which is loaded or <b>null</b> for loading all files in bucket folder. */
	private final String lastFilename;
	
	private final AwsFilesComparator comparator = new AwsFilesComparator();
	
	/** Instantiate file find class.
	 * @param bucket the name of bucket.
	 * @param reportPrefix the prefix assigned to the report when you created the report in AWS.
	 * @param reportName the name of report assigned to the report when you created the report in AWS.
	 * @param assemblyId ID that AWS creates each time that the report is updated.
	 * @param lastFilename the name of file which is loaded last time or <b>null</b> for loading all files in bucket folder.
	 */
	public S3FileList(String bucket, String reportPrefix, String reportName, String assemblyId, String lastFilename) {
		this.bucket = bucket;
		this.reportPrefix = reportPrefix;
		this.reportName = reportName;
		this.assemblyId = assemblyId;
		this.lastFilename = lastFilename;
	}

	/** Return the list of files for new reports.
	 * @param s3Client the S3 client.
	 * @return the list of files.
	 * @throws AdapterException
	 */
	public List<String> getFiles(AmazonS3 s3Client) throws AdapterException {
		final Pattern pattern = Pattern.compile("^report-prefix/report/" + PATTERN_DATE + "-" + PATTERN_DATE +
				S3O_DELIMITER + assemblyId + S3O_DELIMITER + reportName + "-*[0-9]{0,3}\\.csv");//[0-9]");
		final String prefix = (reportPrefix == null || reportPrefix.isEmpty() ?
				reportName + S3O_DELIMITER :
				reportPrefix + S3O_DELIMITER + reportName + S3O_DELIMITER);
		final String startAfter = (lastFilename == null ? prefix : lastFilename);
		
		ListObjectsV2Request request = new ListObjectsV2Request()
				.withBucketName(bucket)
				.withPrefix(prefix)
				.withStartAfter(startAfter);
		ListObjectsV2Result result;
		List<String> list = new ArrayList<>();
		try {
			do {
				result = s3Client.listObjectsV2(request);
				for(S3ObjectSummary item : result.getObjectSummaries()) {
					if (pattern.matcher(item.getKey()).matches()) {
						list.add(item.getKey());
					}
				}
			} while (result.isTruncated());
		} catch (Exception e) {
			throw new AdapterException("Cannot get listing of bucket \"" + bucket + S3O_DELIMITER + prefix + "\". " +
					e.getLocalizedMessage(), e);
		}
		
		sort(list);
		return list;
	}
	
	protected void sort(List<String> list) {
		list.sort(comparator);
	}
	
	
	/** Comparator for sort file list.
	 */
	class AwsFilesComparator implements Comparator<String> {
		
		/** Return the extension of report file.
		 * @param filename the name of file.
		 */
		private String getExt(String filename) {
			int pos = filename.lastIndexOf(".csv");
			return (pos < 0 ? null : filename.substring(pos));
		}
		
		/** Find index of file number and return if found the name of report file without extension and number of file
		 * otherwise filename and -1.
		 * @param filename the name of file.
		 * @param ext the extension of file.
		 * @return pair name of file and index.
		 */
		private Pair<String, Integer> getFileOrder(String filename, String ext) {
			MutablePair<String, Integer> p = new MutablePair<>(filename, -1);
			filename = filename.substring(0, filename.length() - ext.length());
			int pos = filename.lastIndexOf('-');
			if (pos < 0 && pos < filename.length()) {
				return p;
			}
			
			try {
				p.setRight(Integer.parseInt(filename.substring(pos + 1)));
				p.setLeft(filename.substring(0, pos));
				return p;
			} catch (NumberFormatException e) {
				return p;
			}
		}
		
		@Override
		public int compare(String s1, String s2) {
			if (s1 == s2) {
	            return 0;
	        } else if (s1 == null) {
	            return -1;
	        } else if (s2 == null) {
	            return 1;
	        }
			
			String ext = getExt(s1);
			if (ext == null || !s2.endsWith(ext)) {
				return s1.compareTo(s2);
			}
			
			Pair<String, Integer> p1 = getFileOrder(s1, ext);
			if (p1.getValue() == -1) {
				return s1.compareTo(s2);
			}
			Pair<String, Integer> p2 = getFileOrder(s2, ext);
			if (p2.getValue() == -1) {
				return s1.compareTo(s2);
			}
			
			return p1.compareTo(p2);
		}		
	}
}
