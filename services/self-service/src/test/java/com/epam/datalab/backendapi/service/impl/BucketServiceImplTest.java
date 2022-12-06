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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.resources.TestBase;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.dto.bucket.BucketDTO;
import com.epam.datalab.dto.bucket.BucketDeleteDTO;
import com.epam.datalab.dto.bucket.FolderUploadDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BucketServiceImplTest extends TestBase {

    private static final String BUCKET_GET_OBJECTS = "%sbucket/%s";
    private static final String BUCKET_UPLOAD_OBJECT = "%sbucket/upload";
    private static final String BUCKET_UPLOAD_FOLDER = "%sbucket/folder/upload";
    private static final String BUCKET_DOWNLOAD_OBJECT = "%sbucket/%s/object/%s/download";
    private static final String BUCKET_DELETE_OBJECT = "%sbucket/objects/delete";
    private static final String BUCKET = "bucket";
    private static final String OBJECT = "object";
    private static final String SIZE = "size";
    private static final long DATE = Instant.now().toEpochMilli();
    private static final String FOLDER = "folder/";

    @Mock
    private EndpointService endpointService;
    @Mock
    private RESTService provisioningService;
    @InjectMocks
    private BucketServiceImpl bucketService;

    @Test
    public void getObjects() {
        EndpointDTO endpointDTO = getEndpointDTO();
        List<BucketDTO> objects = getBucketList();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.get(anyString(), anyString(), any(GenericType.class))).thenReturn(objects);

        List<BucketDTO> actualObjects = bucketService.getObjects(getUserInfo(), BUCKET, ENDPOINT_NAME);

        assertEquals("lists should be equal", objects, actualObjects);
        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).get(String.format(BUCKET_GET_OBJECTS, ENDPOINT_URL, BUCKET), TOKEN, new GenericType<List<BucketDTO>>() {
        });
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void getObjectsWithException() {
        EndpointDTO endpointDTO = getEndpointDTO();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.get(anyString(), anyString(), any(GenericType.class))).thenThrow(new DatalabException("Exception message"));

        bucketService.getObjects(getUserInfo(), BUCKET, ENDPOINT_NAME);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).get(String.format(BUCKET_GET_OBJECTS, ENDPOINT_URL, BUCKET), TOKEN, new GenericType<List<BucketDTO>>() {
        });

        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test
    public void uploadObject() {
        EndpointDTO endpointDTO = getEndpointDTO();
        Response response = Response.ok().build();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.postForm(anyString(), anyString(), any(FormDataMultiPart.class), any())).thenReturn(response);

        bucketService.uploadObject(getUserInfo(), BUCKET, OBJECT, ENDPOINT_NAME, getInputStream(), APPLICATION_JSON, 0, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).postForm(eq(String.format(BUCKET_UPLOAD_OBJECT, ENDPOINT_URL)), eq(TOKEN), any(FormDataMultiPart.class), eq(Response.class));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void uploadObjectWithException1() {
        EndpointDTO endpointDTO = getEndpointDTO();
        Response response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.postForm(anyString(), anyString(), any(FormDataMultiPart.class), any())).thenReturn(response);

        bucketService.uploadObject(getUserInfo(), BUCKET, OBJECT, ENDPOINT_NAME, getInputStream(), APPLICATION_JSON, 0, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).postForm(eq(String.format(BUCKET_UPLOAD_OBJECT, ENDPOINT_URL)), eq(TOKEN), any(FormDataMultiPart.class), eq(Response.class));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void uploadObjectWithException2() {
        EndpointDTO endpointDTO = getEndpointDTO();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);

        bucketService.uploadObject(getUserInfo(), BUCKET, OBJECT, ENDPOINT_NAME, null, APPLICATION_JSON, 0, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test
    public void uploadFolder() {
        EndpointDTO endpointDTO = getEndpointDTO();
        Response response = Response.ok().build();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.post(anyString(), anyString(), any(FolderUploadDTO.class), any())).thenReturn(response);

        bucketService.uploadFolder(getUserInfo(), BUCKET, FOLDER, ENDPOINT_NAME, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(eq(String.format(BUCKET_UPLOAD_FOLDER, ENDPOINT_URL)), eq(TOKEN), eq(getFolderUploadDTO()), eq(Response.class));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void uploadFolderWithException1() {
        EndpointDTO endpointDTO = getEndpointDTO();
        Response response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.post(anyString(), anyString(), any(FolderUploadDTO.class), any())).thenReturn(response);

        bucketService.uploadFolder(getUserInfo(), BUCKET, FOLDER, ENDPOINT_NAME, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(eq(String.format(BUCKET_UPLOAD_FOLDER, ENDPOINT_URL)), eq(TOKEN), eq(getFolderUploadDTO()), eq(Response.class));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void uploadFolderWithException2() {
        bucketService.uploadFolder(getUserInfo(), BUCKET, "folder_name_without_slash", ENDPOINT_NAME, null);
    }

    @Test
    public void downloadObject() throws IOException {
        EndpointDTO endpointDTO = getEndpointDTO();
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.getWithMediaTypes(anyString(), anyString(), any(), anyString(), anyString())).thenReturn(getInputStream());
        when(response.getOutputStream()).thenReturn(outputStream);

        bucketService.downloadObject(getUserInfo(), BUCKET, OBJECT, ENDPOINT_NAME, response, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).getWithMediaTypes(eq(String.format(BUCKET_DOWNLOAD_OBJECT, ENDPOINT_URL, BUCKET, OBJECT)), eq(TOKEN), eq(InputStream.class),
                eq(APPLICATION_JSON), eq(APPLICATION_OCTET_STREAM));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void downloadObjectWithException() {
        EndpointDTO endpointDTO = getEndpointDTO();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.getWithMediaTypes(anyString(), anyString(), any(), anyString(), anyString())).thenThrow(new DatalabException("Exception message"));

        bucketService.downloadObject(getUserInfo(), BUCKET, OBJECT, ENDPOINT_NAME, response, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).getWithMediaTypes(eq(String.format(BUCKET_DOWNLOAD_OBJECT, ENDPOINT_URL, BUCKET, OBJECT)), eq(TOKEN), eq(InputStream.class),
                eq(APPLICATION_JSON), eq(APPLICATION_OCTET_STREAM));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test
    public void deleteObjects() {
        EndpointDTO endpointDTO = getEndpointDTO();
        Response response = Response.ok().build();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.post(anyString(), anyString(), any(BucketDeleteDTO.class), any())).thenReturn(response);

        bucketService.deleteObjects(getUserInfo(), BUCKET, Collections.singletonList(OBJECT), ENDPOINT_NAME, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(eq(String.format(BUCKET_DELETE_OBJECT, ENDPOINT_URL)), eq(TOKEN), eq(getBucketDeleteDTO()), eq(Response.class));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    @Test(expected = DatalabException.class)
    public void deleteObjectsWithException() {
        EndpointDTO endpointDTO = getEndpointDTO();
        Response response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
        when(endpointService.get(anyString())).thenReturn(endpointDTO);
        when(provisioningService.post(anyString(), anyString(), any(BucketDeleteDTO.class), any())).thenReturn(response);

        bucketService.deleteObjects(getUserInfo(), BUCKET, Collections.singletonList(OBJECT), ENDPOINT_NAME, null);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(eq(String.format(BUCKET_DELETE_OBJECT, ENDPOINT_URL)), eq(TOKEN), eq(getBucketDeleteDTO()), eq(Response.class));
        verifyNoMoreInteractions(endpointService, provisioningService);
    }

    private List<BucketDTO> getBucketList() {
        return Collections.singletonList(BucketDTO.builder()
                .bucket(BUCKET)
                .object(OBJECT)
                .size(SIZE)
                .lastModifiedDate(DATE)
                .build());
    }

    private FolderUploadDTO getFolderUploadDTO() {
        return new FolderUploadDTO(BUCKET, FOLDER);
    }

    private BucketDeleteDTO getBucketDeleteDTO() {
        return new BucketDeleteDTO(BUCKET, Collections.singletonList(OBJECT));
    }

    private ByteArrayInputStream getInputStream() {
        return new ByteArrayInputStream("input stream".getBytes());
    }
}