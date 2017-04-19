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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.dlab.core.AdapterBase;
import com.epam.dlab.core.parser.CommonFormat;
import com.epam.dlab.core.parser.ReportLine;
import com.epam.dlab.exception.AdapterException;
import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;

/** The adapter for S3 file system of Amazon.
 */
@JsonTypeName(ModuleName.ADAPTER_S3_FILE)
@JsonClassDescription(
	"Amazon S3 file system adapter.\n" +
	"Read source or write converted data to the file in Amazon S3 bucket.\n" +
	"  - type: " + ModuleName.ADAPTER_S3_FILE + "\n" +
	"    [writeHeader: <true | false>] - write header of data to the adapterOut.\n" +
	"    bucket: <bucketname>          - the name of S3 bucket.\n" +
	"    prefix: <path>                - the path to report.\n" +
	"    report: <reportname>          - the name of report.\n" +
	"    assemblyId: <id>              - the ID of linked account.\n" +
	"    [accessKeyId: <string>]       - Amazon access key ID.\n" +
	"    [secretAccessKey: <string>]   - Amazon secret access key."
	)
public class AdapterS3File extends AdapterBase {
	
	/** Name of key for last loaded file. */
	public static final String DATA_KEY_LAST_LOADED_FILE = "maxStartDate";

	/** The name of bucket. */
	@NotNull
	@JsonProperty
	private String bucket;

	/** Prefix assigned to the report when you created the report in AWS. */
	@JsonProperty
	private String prefix;
	
	/** Name of report assigned to the report when you created the report in AWS. */
	@NotNull
	@JsonProperty
	private String report;
	
	/** ID that AWS creates each time that the report is updated. */
	@NotNull
	@JsonProperty
	private String assemblyId;

	/** Access key ID for Amazon Web Services. */
	@JsonProperty
	private String accessKeyId;

	/** Secret key for Amazon Web Services. */
	@JsonProperty
	private String secretAccessKey;

	
	/** Default constructor for deserialization. */
	public AdapterS3File() { }
	
	/** Instantiate adapter for reading or writing.
	 * @param mode the mode of adapter.
	 */
	public AdapterS3File(Mode mode) {
		super(mode);
	}


	/** Return the name of bucket. */
	public String getBucket() {
		return bucket;
	}
	
	/** Set the name of bucket. */
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	/** Return the prefix assigned to the report when you created the report in AWS. */
	public String getPrefix() {
		return prefix;
	}
	
	/** Set the prefix assigned to the report when you created the report in AWS. */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/** Return the name of report assigned to the report when you created the report in AWS. */
	public String getReport() {
		return report;
	}
	
	/** Set the name of report assigned to the report when you created the report in AWS. */
	public void setReport(String report) {
		this.report = report;
	}

	/** Return the ID that AWS creates each time that the report is updated. */
	public String getAssemblyId() {
		return assemblyId;
	}
	
	/** Set the ID that AWS creates each time that the report is updated. */
	public void setAssemblyId(String assemblyId) {
		this.assemblyId = assemblyId;
	}

	/** Return the access key ID for Amazon Web Services. */
	public String getAccessKeyId() {
		return this.accessKeyId;
	}

	/** Set the access key ID for Amazon Web Services. */
	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	/** Return the secret key for Amazon Web Services. */
	public String getSecretAccessKey() {
		return this.secretAccessKey;
	}

	/** Set the secret key for Amazon Web Services. */
	public void setSecretAccessKey(String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
	}

	
	/** Reader for adapter. */
	@JsonIgnore
	private BufferedReader reader;
	
	/** List of report files for loading. */
	private List<String> filelist;
	
	/** Index of current report file. */
	private int currentFileIndex;
	
	/** Creates and returns the Amazon client, as well as checks bucket existence.
	 * @throws AdapterException
	 */
	private AmazonS3 getAmazonClient() throws AdapterException {
		AmazonS3 s3 = (accessKeyId == null ?
						new AmazonS3Client() :
						new AmazonS3Client(new BasicAWSCredentials(accessKeyId, secretAccessKey)));
		
		if (!s3.doesBucketExist(bucket)) {
			throw new AdapterException("Bucket \"" + bucket + "\" does not exist.");
		}
		
		return s3;
	}
	
	private String getCurrentFileName() {
		return (filelist == null || currentFileIndex < 0 ? null : filelist.get(currentFileIndex));
	}
	
	/** Open the source file and return reader.
	 * @param filename the name of file.
	 * @throws AdapterException
	 */
	private BufferedReader getReader() throws AdapterException {
		AmazonS3 s3 = getAmazonClient();
		try {
			GetObjectRequest request = new GetObjectRequest(bucket, getCurrentFileName());
			S3Object object = s3.getObject(request);
			return new BufferedReader(
						new InputStreamReader(
							object.getObjectContent()));
		} catch (Exception e) {
			throw new AdapterException("Cannot open file " + bucket + "/" + getCurrentFileName() + ". " + e.getLocalizedMessage(), e);
		}
	}
	
	@Override
	public void open() throws AdapterException {
		if (getMode() == Mode.READ) {
			S3FileList s3files = new S3FileList(bucket, prefix, report, assemblyId, getModuleData().get(DATA_KEY_LAST_LOADED_FILE));
			filelist = s3files.getFiles(getAmazonClient());
			currentFileIndex = (filelist.size() == 0 ? -1 : 0);
			reader = getReader();
		} else if (getMode() == Mode.WRITE) {
			throw new AdapterException("Write mode is unimplemented");
		} else {
			throw new AdapterException("Mode of adapter unknown or not defined. Set mode to " + Mode.READ + " or " + Mode.WRITE + ".");
		}
	}

	@Override
	public void close() throws AdapterException {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				throw new AdapterException("Cannot close file " + getCurrentFileName() + ". " + e.getLocalizedMessage(), e);
			} finally {
				reader = null;
				currentFileIndex++;
			}
		}
	}

	@Override
	public String readLine() throws AdapterException {
		try {
			String line = reader.readLine();
			if (line == null) {
				line = getLineFromNextFile();
			}
			return line;
		} catch (IOException e) {
			throw new AdapterException("Cannot read file " + getCurrentFileName() + ". " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void writeHeader(List<String> header) throws AdapterException {
		throw new AdapterException("Unimplemented method");
	}
	
	@Override
	public void writeRow(ReportLine row) throws AdapterException {
		throw new AdapterException("Unimplemented method");
	}
	
	
	private String getLineFromNextFile() throws AdapterException {
		close();
		currentFileIndex++;
		if (currentFileIndex >= filelist.size()) {
			close();
			return null;
		}
		reader = getReader();
		//TODO Skip lines
		return null;
	}
	
	@Override
	public ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("bucket", bucket)
				.add("prefix", prefix)
				.add("report", report)
				.add("assemblyId", assemblyId)
				.add("accessKeyId", accessKeyId)
				.add("secretAccessKey", secretAccessKey);
	}
}
