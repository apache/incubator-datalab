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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.dlab.core.AdapterBase;
import com.epam.dlab.exceptions.AdapterException;
import com.epam.dlab.model.aws.ReportLine;
import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The adapter for S3 file system of Amazon.
 */
@JsonTypeName(ModuleName.ADAPTER_S3_FILE)
@JsonClassDescription(
		"Amazon S3 file system adapter.\n" +
				"Read source or write converted data to the file in Amazon S3 bucket.\n" +
				"  - type: " + ModuleName.ADAPTER_S3_FILE + "\n" +
				"    [writeHeader: <true | false>]   - write header of data to the adapterOut.\n" +
				"    bucket: <bucketname>            - the name of S3 bucket.\n" +
				"    path: <path>                    - the path to the report or empty if used the root folder.\n" +
				"    accountId: <AWS account number> - the account number, see for details\n" +
				"                                      \"Detailed billing report with resources and tags\"\n" +
				"                                      http://docs.aws.amazon" +
				".com/awsaccountbilling/latest/aboutv2/billing-reports.html#detailed-report-with-resources-tags\n" +
				"    [accessKeyId: <string>]         - Amazon access key ID.\n" +
				"    [secretAccessKey: <string>]     - Amazon secret access key."
)
public class AdapterS3File extends AdapterBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(AdapterS3File.class);

	/**
	 * Name of key for the last loaded file.
	 */
	public static final String DATA_KEY_LAST_LOADED_FILE = "AdapterS3File_lastLoadedFile";

	/**
	 * Name of key for the modification date of loaded file.
	 */
	public static final String DATA_KEY_LAST_MODIFICATION_DATE = "AdapterS3File_lastModifyDate";
	private static final String CANNOT_READ_FILE_FORMAT = "Cannot read file %s. %s";
	private static final String DELIMITER = "/";

	/**
	 * The name of bucket.
	 */
	@NotNull
	@JsonProperty
	private String bucket;

	/**
	 * The path to report.
	 */
	@JsonProperty
	private String path;

	/**
	 * AWS account number.
	 */
	@NotNull
	@JsonProperty
	private String accountId;

	/**
	 * Access key ID for Amazon Web Services.
	 */
	@JsonProperty
	private String accessKeyId;

	/**
	 * Secret key for Amazon Web Services.
	 */
	@JsonProperty
	private String secretAccessKey;

	@JsonProperty
	private boolean awsJobEnabled;

	@JsonProperty
	private String region;

	/**
	 * Return the name of bucket.
	 */
	public String getBucket() {
		return bucket;
	}

	/**
	 * Set the name of bucket.
	 */
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	/**
	 * Return the path to report.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path to report.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Return the AWS account number.
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * Set the AWS account number.
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * Return the access key ID for Amazon Web Services.
	 */
	public String getAccessKeyId() {
		return this.accessKeyId;
	}

	/**
	 * Set the access key ID for Amazon Web Services.
	 */
	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	/**
	 * Return the secret key for Amazon Web Services.
	 */
	public String getSecretAccessKey() {
		return this.secretAccessKey;
	}

	/**
	 * Set the secret key for Amazon Web Services.
	 */
	public void setSecretAccessKey(String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
	}


	/**
	 * List of report files for loading.
	 */
	@JsonIgnore
	private List<String> filelist = null;

	/**
	 * Index of current report file.
	 */
	@JsonIgnore
	private int currentFileIndex = -1;

	/**
	 * Index of current report file.
	 */
	@JsonIgnore
	private String entryName = null;

	/**
	 * Amazon S3 client.
	 */
	@JsonIgnore
	private AmazonS3 clientS3 = null;

	/**
	 * Amazon S3 client.
	 */
	@JsonIgnore
	private Date lastModificationDate = null;

	/**
	 * File input stream.
	 */
	@JsonIgnore
	private InputStream fileInputStream = null;

	/**
	 * Zip input stream.
	 */
	@JsonIgnore
	private ZipInputStream zipInputStream;

	/**
	 * Reader for adapter.
	 */
	@JsonIgnore
	private BufferedReader reader = null;

	@Override
	public void open() throws AdapterException {
		LOGGER.debug("Adapter S3 will be opened for {}", getMode());
		if (getMode() == Mode.READ) {
			setLastModificationDate();
			clientS3 = getAmazonClient();
			S3FileList s3files = new S3FileList(awsJobEnabled, bucket, getModuleData());
			filelist = s3files.getFiles(clientS3);
			currentFileIndex = (filelist.isEmpty() ? -1 : 0);
			fileInputStream = null;
			reader = null;
			entryName = null;
			openNextEntry();
			LOGGER.debug("Adapter S3 has been opened");
		} else if (getMode() == Mode.WRITE) {
			throw new AdapterException("Unsupported mode " + Mode.WRITE + ".");
		} else {
			throw new AdapterException("Mode of adapter unknown or not defined. Set mode to " + Mode.READ + ".");
		}
	}

	@Override
	public boolean hasMultyEntry() {
		return true;
	}

	@Override
	public boolean openNextEntry() throws AdapterException {
		String filename = getCurrentFileName();
		if (filename == null) {
			if (filelist.isEmpty()) {
				final String reportPath = path == null ? bucket : bucket + DELIMITER + path;
				LOGGER.debug("New report files in bucket folder {} not found", reportPath);
			}
			return false;
		}
		entryName = filename;
		LOGGER.debug("Open a next entry in file {}", filename);

		if (fileInputStream == null) {
			openZipFile(filename);
		}
		if (openZipEntry(filename)) {
			return true;
		}

		if (reader != null) {
			try {
				getModuleData().setId(filename);
				getModuleData().setModificationDate(lastModificationDate);
				getModuleData().set(DATA_KEY_LAST_LOADED_FILE, filename);
				getModuleData().set(DATA_KEY_LAST_MODIFICATION_DATE, lastModificationDate);
				getModuleData().store();
			} catch (Exception e) {
				throw new AdapterException(e.getLocalizedMessage(), e);
			}
		}

		reader = null;
		currentFileIndex++;
		return openNextEntry();
	}

	@Override
	public boolean hasEntryData() {
		return (reader != null);
	}

	@Override
	public void close() throws AdapterException {
		closeZipFile(getCurrentFileName());
	}

	@Override
	public String getEntryName() {
		return entryName;
	}

	@Override
	public String readLine() throws AdapterException {
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new AdapterException(String.format(CANNOT_READ_FILE_FORMAT, getCurrentFileName(), e
					.getLocalizedMessage()), e);
		}
	}

	@Override
	public void writeHeader(List<String> header) throws AdapterException {
		throw new AdapterException("Unimplemented method.");
	}

	@Override
	public void writeRow(ReportLine row) throws AdapterException {
		throw new AdapterException("Unimplemented method.");
	}


	/**
	 * Return the current file name.
	 */
	public String getCurrentFileName() {
		return (filelist == null || currentFileIndex < 0 || currentFileIndex >= filelist.size() ? null : filelist.get
				(currentFileIndex));
	}

	/**
	 * Creates and returns the Amazon client, as well as checks bucket existence.
	 *
	 * @throws AdapterException
	 */
	private AmazonS3 getAmazonClient() throws AdapterException {
		AmazonS3 s3 = (accessKeyId == null ?
				new AmazonS3Client() :
				new AmazonS3Client(new BasicAWSCredentials(accessKeyId, secretAccessKey)));

		Optional.ofNullable(region)
				.filter(StringUtils::isNoneBlank)
				.ifPresent(r -> s3.setRegion(Region.fromValue(r).toAWSRegion()));
		if (!s3.doesBucketExist(bucket)) {
			throw new AdapterException("Bucket \"" + bucket + "\" does not exist.");
		}

		return s3;
	}

	/**
	 * Open the source file and return reader.
	 *
	 * @throws AdapterException
	 */
	private InputStream getFileStream() throws AdapterException {
		try {
			GetObjectRequest request = new GetObjectRequest(bucket, getCurrentFileName());
			S3Object object = clientS3.getObject(request);
			lastModificationDate = object.getObjectMetadata().getLastModified();
			return object.getObjectContent();
		} catch (Exception e) {
			throw new AdapterException("Cannot open file " + bucket + DELIMITER + getCurrentFileName() + ". " + e
					.getLocalizedMessage(), e);
		}
	}

	/**
	 * Return the modification date of loaded file.
	 *
	 * @throws AdapterException
	 */
	private void setLastModificationDate() throws AdapterException {
		try {
			lastModificationDate = getModuleData().getDate(DATA_KEY_LAST_MODIFICATION_DATE);
		} catch (Exception e) {
			throw new AdapterException("Cannot get the last modification date for report. " + e.getLocalizedMessage(),
					e);
		}
	}

	/**
	 * Open a zip file.
	 *
	 * @param filename file name.
	 * @throws AdapterException
	 */
	private void openZipFile(String filename) throws AdapterException {
		LOGGER.debug("Open a zip file {}", filename);
		try {
			fileInputStream = getFileStream();
		} catch (Exception e) {
			throw new AdapterException("Cannot open file " + filename + ". " + e.getLocalizedMessage(), e);
		}

		try {
			zipInputStream = new ZipInputStream(fileInputStream);
		} catch (Exception e) {
			throw new AdapterException(String.format(CANNOT_READ_FILE_FORMAT, filename, e.getLocalizedMessage()), e);
		}
	}

	/**
	 * Open a next entry in zip file.
	 *
	 * @param filename file name.
	 * @return <b>true</b> if a next entry has been opened.
	 * @throws AdapterException
	 */
	private boolean openZipEntry(String filename) throws AdapterException {
		ZipEntry entry;
		try {
			entry = zipInputStream.getNextEntry();
			if (entry != null) {
				entryName = filename + ":" + entry.getName();
				LOGGER.debug("Next the zip entry {}", entry.getName());
				reader = new BufferedReader(new InputStreamReader(zipInputStream));
			} else {
				LOGGER.debug("Zip file have no more entries");
			}
		} catch (Exception e) {
			throw new AdapterException(String.format(CANNOT_READ_FILE_FORMAT, filename, e.getLocalizedMessage()), e);
		}
		if (entry == null) {
			closeZipFile(filename);
		}
		return (entry != null);
	}

	/**
	 * Close a zip file.
	 *
	 * @param filename file name.
	 * @throws AdapterException
	 */
	private void closeZipFile(String filename) throws AdapterException {
		if (fileInputStream != null) {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				throw new AdapterException("Cannot close file " + filename + ". " + e.getLocalizedMessage(), e);
			}
			fileInputStream = null;
		}
	}


	@Override
	public ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("bucket", bucket)
				.add("path", path)
				.add("accountId", accountId)
				.add("accessKeyId", "***")
				.add("secretAccessKey", "***");
	}
}
