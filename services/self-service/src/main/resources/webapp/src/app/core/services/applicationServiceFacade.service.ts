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

import { Injectable } from '@angular/core';
import { Http, Response, RequestOptions, RequestMethod, Headers } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import { Dictionary } from '../util';

@Injectable()
export class ApplicationServiceFacade {

  private static readonly LOGIN = 'login';
  private static readonly LOGOUT = 'logout';
  private static readonly AUTHORIZE = 'authorize';
  private static readonly OAUTH = 'oauth';
  private static readonly ACCESS_KEY = 'access_key';
  private static readonly ACCESS_KEY_GENERATE = 'access_key_generate';
  private static readonly PROVISIONED_RESOURCES = 'provisioned_resources';
  private static readonly EXPLORATORY_ENVIRONMENT = 'exploratory_environment';
  private static readonly IMAGE = 'image';
  private static readonly SCHEDULER = 'scheduler';
  private static readonly EXPLORATORY_ENVIRONMENT_TEMPLATES = 'exploratory_templates';
  private static readonly COMPUTATIONAL_RESOURCES_TEMLATES = 'computational_templates';
  private static readonly COMPUTATIONAL_RESOURCES = 'computational_resources';
  private static readonly COMPUTATIONAL_RESOURCES_DATAENGINE = 'computational_resources_dataengine';
  private static readonly COMPUTATIONAL_RESOURCES_DATAENGINESERVICE = 'computational_resources_dataengineservice';
  private static readonly USER_PREFERENCES = 'user_preferences';
  private static readonly ENVIRONMENT_HEALTH_STATUS = 'environment_health_status';
  private static readonly BACKUP = 'backup';
  private static readonly EDGE_NODE_START = 'edge_node_start';
  private static readonly EDGE_NODE_STOP = 'edge_node_stop';
  private static readonly EDGE_NODE_RECREATE = 'edge_node_recreate';
  private static readonly LIB_GROUPS = 'lib_groups';
  private static readonly LIB_LIST = 'lib_list';
  private static readonly LIB_INSTALL = 'lib_install';
  private static readonly INSTALLED_LIBS_FORMAT = 'installed_libs_format';
  private static readonly INSTALLED_LIBS = 'installed_libs';
  private static readonly GIT_CREDS = 'git_creds';
  private static readonly BILLING = 'billing';
  private static readonly DOWNLOAD_REPORT = 'download_report';
  private accessTokenKey: string = 'access_token';
  private requestRegistry: Dictionary<string>;

  constructor(private http: Http) {
    this.setupRegistry();
  }

  public buildLoginRequest(body: any): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LOGIN),
      body,
      this.getRequestOptions(true, false));
  }

  public buildLogoutRequest(): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LOGOUT),
      '',
      this.getRequestOptions(true, true));
  }

  public buildAuthorizeRequest(body: any): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.AUTHORIZE),
      body,
      this.getRequestOptions(true, true));
  }

  public buildGetAuthToken(body: any): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.OAUTH),
      body,
      this.getRequestOptions(true, true));
  }

  public buildCheckUserAccessKeyRequest(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY),
      null,
      this.getRequestOptions(true, true));
  }

  public buildGenerateAccessKey(): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY_GENERATE),
      null,
      this.getRequestOptions(true, true));
  }

  public buildUploadUserAccessKeyRequest(body: any): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY),
      body,
      this.getRequestOptions(false, true));
  }

  public buildGetUserProvisionedResourcesRequest(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.PROVISIONED_RESOURCES),
      null,
      this.getRequestOptions(true, true));
  }

  public buildGetExploratoryEnvironmentTemplatesRequest(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT_TEMPLATES),
      null,
      this.getRequestOptions(true, true));
  }

  public buildGetComputationalResourcesTemplatesRequest(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_TEMLATES),
      null,
      this.getRequestOptions(true, true));
  }

  public buildCreateExploratoryEnvironmentRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT),
      data,
      this.getRequestOptions(true, true));
  }

  public buildRunExploratoryEnvironmentRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT),
      data,
      this.getRequestOptions(true, true));
  }

  public buildSuspendExploratoryEnvironmentRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT),
      data,
      this.getRequestOptions(true, true));
  }

  public buildCreateComputationalResources_DataengineServiceRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_DATAENGINESERVICE),
      data,
      this.getRequestOptions(true, true));
  }

  public buildCreateComputationalResources_DataengineRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_DATAENGINE),
      data,
      this.getRequestOptions(true, true));
  }

  public buildDeleteComputationalResourcesRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetUserPreferences(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.USER_PREFERENCES),
      null,
      this.getRequestOptions(true, true));
  }

  public buildUpdateUserPreferences(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.USER_PREFERENCES),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetEnvironmentHealthStatus(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS),
      null,
      this.getRequestOptions(true, true));
  }

  public buildGetEnvironmentStatuses(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS),
      data,
      this.getRequestOptions(true, true));
  }

  public buildRunEdgeNodeRequest(): Observable<Response>  {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EDGE_NODE_START),
      null,
      this.getRequestOptions(true, true));
  }

  public buildSuspendEdgeNodeRequest(): Observable<Response>  {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EDGE_NODE_STOP),
      null,
      this.getRequestOptions(true, true));
  }

  public buildRecreateEdgeNodeRequest(): Observable<Response>  {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EDGE_NODE_RECREATE),
      null,
      this.getRequestOptions(true, true));
  }

  public buildGetGroupsList(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_GROUPS),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetAvailableLibrariesList(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_LIST),
      data,
      this.getRequestOptions(true, true));
  }

  public buildInstallLibraries(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_INSTALL),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetInstalledLibrariesList(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.INSTALLED_LIBS_FORMAT),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetInstalledLibsByResource(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.INSTALLED_LIBS),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetGitCreds(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.GIT_CREDS),
      null,
      this.getRequestOptions(true, true));
  }

  public buildUpdateGitCredentials(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.GIT_CREDS),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetGeneralBillingData(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.BILLING),
      data,
      this.getRequestOptions(true, true));
  }

  public buildDownloadReportData(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.DOWNLOAD_REPORT),
      data,
      this.getRequestOptions(true, true));
  }

  public buildCreateBackupRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.BACKUP),
      data,
      this.getRequestOptions(true, true));
  }
  public buildGetBackupStatusRequest(uuid): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.BACKUP),
      uuid,
      this.getRequestOptions(true, true));
  }

  public buildGetUserImages(image): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.IMAGE),
      image,
      this.getRequestOptions(true, true));
  }

  public buildGetImagesList(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.IMAGE),
      null,
      this.getRequestOptions(true, true));
  }

  public buildCreateAMI(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.IMAGE),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetExploratorySchedule(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.SCHEDULER),
      data,
      this.getRequestOptions(true, true));
  }

  public buildSetExploratorySchedule(param, data): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.SCHEDULER) + param,
      data,
      this.getRequestOptions(true, true));
  }

  private setupRegistry(): void {
    this.requestRegistry = new Dictionary<string>();

    // Security
    this.requestRegistry.Add(ApplicationServiceFacade.LOGIN, '/api/user/login');
    this.requestRegistry.Add(ApplicationServiceFacade.LOGOUT, '/api/user/logout');
    this.requestRegistry.Add(ApplicationServiceFacade.AUTHORIZE, '/api/user/authorize');

    this.requestRegistry.Add(ApplicationServiceFacade.OAUTH, '/api/user/azure/oauth');
    this.requestRegistry.Add(ApplicationServiceFacade.ACCESS_KEY, '/api/user/access_key');
    this.requestRegistry.Add(ApplicationServiceFacade.ACCESS_KEY_GENERATE, '/api/user/access_key/generate');

    // Exploratory Environment
    this.requestRegistry.Add(ApplicationServiceFacade.PROVISIONED_RESOURCES,
      '/api/infrastructure/info');
    this.requestRegistry.Add(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT,
      '/api/infrastructure_provision/exploratory_environment');
    this.requestRegistry.Add(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT_TEMPLATES,
      '/api/infrastructure_templates/exploratory_templates');
    this.requestRegistry.Add(ApplicationServiceFacade.IMAGE,
      '/api/infrastructure_provision/exploratory_environment/image');
    this.requestRegistry.Add(ApplicationServiceFacade.SCHEDULER,
      '/api/infrastructure_provision/exploratory_environment/scheduler');


    // Computational Resources
    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES,
      '/api/infrastructure_provision/computational_resources');
    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_DATAENGINESERVICE,
      '/api/infrastructure_provision/computational_resources/dataengine-service'); // emr(aws)
    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_DATAENGINE,
      '/api/infrastructure_provision/computational_resources/dataengine'); // spark (azure|aws)

    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_TEMLATES,
      '/api/infrastructure_templates/computational_templates');

    // Filtering Configuration
    this.requestRegistry.Add(ApplicationServiceFacade.USER_PREFERENCES, '/api/user/settings');

    // Environment Health Status
    this.requestRegistry.Add(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS, '/api/infrastructure/status');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_START, '/api/infrastructure/edge/start');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_STOP, '/api/infrastructure/edge/stop');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_RECREATE, '/api/user/access_key/recover');
    this.requestRegistry.Add(ApplicationServiceFacade.BACKUP, '/api/infrastructure/backup');

    // Libraries Installation
    this.requestRegistry.Add(ApplicationServiceFacade.LIB_GROUPS, '/api/infrastructure_provision/exploratory_environment/lib_groups');
    this.requestRegistry.Add(ApplicationServiceFacade.LIB_LIST, '/api/infrastructure_provision/exploratory_environment/search/lib_list');
    this.requestRegistry.Add(ApplicationServiceFacade.LIB_INSTALL, '/api/infrastructure_provision/exploratory_environment/lib_install');
    this.requestRegistry.Add(ApplicationServiceFacade.INSTALLED_LIBS_FORMAT,
      '/api/infrastructure_provision/exploratory_environment/lib_list/formatted');
    this.requestRegistry.Add(ApplicationServiceFacade.INSTALLED_LIBS, '/api/infrastructure_provision/exploratory_environment/lib_list');

    // UnGit credentials
    this.requestRegistry.Add(ApplicationServiceFacade.GIT_CREDS, '/api/user/git_creds');

    // billing report
    this.requestRegistry.Add(ApplicationServiceFacade.BILLING, '/api/billing/report');
    this.requestRegistry.Add(ApplicationServiceFacade.DOWNLOAD_REPORT, '/api/billing/report/download');
  }

  private buildRequest(method: RequestMethod, url: string, body: any, opt: RequestOptions): Observable<Response> {
    if (method === RequestMethod.Post) {
      return this.http.post(url, body, opt);
    } else if (method === RequestMethod.Delete) {
      return this.http.delete(body ? url + JSON.parse(body) : url, opt);
    } else if (method === RequestMethod.Put) {
      return this.http.put(url, body, opt);
    } else return this.http.get(body ? (url + body) : url, opt);
  }

  private getRequestOptions(json: boolean, auth: boolean): RequestOptions {
    const headers = new Headers();
    if (json)
      headers.append('Content-type', 'application/json; charset=utf-8');
    if (auth)
      headers.append('Authorization', 'Bearer ' + localStorage.getItem(this.accessTokenKey));
    const reqOpt = new RequestOptions({ headers: headers });
    return reqOpt;
  }
}
