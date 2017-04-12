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
	"    file: <filename>              - the name of file.\n" +
	"    [accessKeyId: <string>]       - Amazon access key ID.\n" +
	"    [secretAccessKey: <string>]   - Amazon secret access key."
	)
public class AdapterS3File extends AdapterBase {

	/** The name of bucket. */
	@NotNull
	@JsonProperty
	private String bucket;

	/** The name of file. */
	@NotNull
	@JsonProperty
	private String file;
	
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

	/** Return the name of file. */
	public String getFile() {
		return file;
	}
	
	/** Set the name of file. */
	public void setFile(String file) {
		this.file = file;
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
	
	/** Writer for adapter. */
	@JsonIgnore
	private BufferedWriter writer;
	
	/** Temporary file for writing.*/
	private File tempFile;

	
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
	
	/** Open the source file and return reader.
	 * @throws AdapterException
	 */
	private BufferedReader getReader() throws AdapterException {
		AmazonS3 s3 = getAmazonClient();
		try {
			GetObjectRequest request = new GetObjectRequest(bucket, getFile());
			S3Object object = s3.getObject(request);
			return new BufferedReader(
						new InputStreamReader(
							object.getObjectContent()));
		} catch (Exception e) {
			throw new AdapterException("Cannot open file " + bucket + "/" + file + ". " + e.getLocalizedMessage(), e);
		}
	}
	
	/** Open the target file and return writer.
	 * @throws AdapterException
	 */
	private BufferedWriter getWriter() throws AdapterException {
		try {
			int pos = Math.max(getFile().indexOf('/'), getFile().indexOf('\\')) + 1;
			String filename = (pos > 0 ? getFile().substring(pos) : "billing.csv");
			tempFile = new File(System.getProperty("java.io.tmpdir"), filename);
			return new BufferedWriter(
						new FileWriter(tempFile));
		} catch (Exception e) {
			throw new AdapterException("Cannot open file " + tempFile.getAbsolutePath() + ". " + e.getLocalizedMessage(), e);
		}
	}
	
	@Override
	public void open() throws AdapterException {
		if (getMode() == Mode.READ) {
			reader = getReader();
		} else if (getMode() == Mode.WRITE) {
			writer = getWriter();
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
				throw new AdapterException("Cannot close file " + file + ". " + e.getLocalizedMessage(), e);
			} finally {
				reader = null;
			}
		}
		
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				throw new AdapterException("Cannot close file " + tempFile.getAbsolutePath() + ". " + e.getLocalizedMessage(), e);
			} finally {
				writer = null;
			}
			AmazonS3 s3 = getAmazonClient();
			try {
		        s3.putObject(new PutObjectRequest(bucket, file, tempFile));
			} catch (Exception e) {
				throw new AdapterException("Cannot copy file \"" + tempFile.getAbsolutePath() +
						"\" to \"" + bucket + "/" + file + "\"" + e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	public String readLine() throws AdapterException {
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new AdapterException("Cannot read file " + file + ". " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void writeHeader(List<String> header) throws AdapterException {
		try {
			writer.write(CommonFormat.rowToString(header));
			writer.write(System.lineSeparator());
		} catch (IOException e) {
			throw new AdapterException("Cannot write file " + tempFile.getAbsolutePath() + ". " + e.getLocalizedMessage(), e);
		}
	}
	
	@Override
	public void writeRow(ReportLine row) throws AdapterException {
		try {
			writer.write(CommonFormat.rowToString(row));
			writer.write(System.lineSeparator());
		} catch (IOException e) {
			throw new AdapterException("Cannot write file " + tempFile.getAbsolutePath() + ". " + e.getLocalizedMessage(), e);
		}
	}
	
	
	@Override
	public ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("bucket", bucket)
				.add("file", file)
				.add("accessKeyId", accessKeyId)
				.add("secretAccessKey", secretAccessKey);
	}
}
