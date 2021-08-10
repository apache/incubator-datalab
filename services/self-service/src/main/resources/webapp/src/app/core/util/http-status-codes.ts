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

export class HTTP_STATUS_CODES {
	public static readonly ACCEPTED = 202;
	public static readonly BAD_GATEWAY = 502;
	public static readonly BAD_REQUEST = 400;
	public static readonly CONFLICT = 409;
	public static readonly CONTINUE = 100;
	public static readonly CREATED = 201;
	public static readonly EXPECTATION_FAILED = 417;
	public static readonly FAILED_DEPENDENCY = 424;
	public static readonly FORBIDDEN = 403;
	public static readonly GATEWAY_TIMEOUT = 504;
	public static readonly GONE = 410;
	public static readonly HTTP_VERSION_NOT_SUPPORTED = 505;
	public static readonly INSUFFICIENT_SPACE_ON_RESOURCE = 419;
	public static readonly INSUFFICIENT_STORAGE = 507;
	public static readonly INTERNAL_SERVER_ERROR = 500;
	public static readonly LENGTH_REQUIRED = 411;
	public static readonly LOCKED = 423;
	public static readonly METHOD_FAILURE = 420;
	public static readonly METHOD_NOT_ALLOWED = 405;
	public static readonly MOVED_PERMANENTLY = 301;
	public static readonly MOVED_TEMPORARILY = 302;
	public static readonly MULTI_STATUS = 207;
	public static readonly MULTIPLE_CHOICES = 300;
	public static readonly NETWORK_AUTHENTICATION_REQUIRED = 511;
	public static readonly NO_CONTENT = 204;
	public static readonly NON_AUTHORITATIVE_INFORMATION = 203;
	public static readonly NOT_ACCEPTABLE = 406;
	public static readonly NOT_FOUND = 404;
	public static readonly NOT_IMPLEMENTED = 501;
	public static readonly NOT_MODIFIED = 304;
	public static readonly OK = 200;
	public static readonly PARTIAL_CONTENT = 206;
	public static readonly PAYMENT_REQUIRED = 402;
	public static readonly PERMANENT_REDIRECT = 308;
	public static readonly PRECONDITION_FAILED = 412;
	public static readonly PRECONDITION_REQUIRED = 428;
	public static readonly PROCESSING = 102;
	public static readonly PROXY_AUTHENTICATION_REQUIRED = 407;
	public static readonly REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
	public static readonly REQUEST_TIMEOUT = 408;
	public static readonly REQUEST_TOO_LONG = 413;
	public static readonly REQUEST_URI_TOO_LONG = 414;
	public static readonly REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	public static readonly RESET_CONTENT = 205;
	public static readonly SEE_OTHER = 303;
	public static readonly SERVICE_UNAVAILABLE = 503;
	public static readonly SWITCHING_PROTOCOLS = 101;
	public static readonly TEMPORARY_REDIRECT = 307;
	public static readonly TOO_MANY_REQUESTS = 429;
	public static readonly UNAUTHORIZED = 401;
	public static readonly UNPROCESSABLE_ENTITY = 422;
	public static readonly UNSUPPORTED_MEDIA_TYPE = 415;
	public static readonly USE_PROXY = 305;
}
