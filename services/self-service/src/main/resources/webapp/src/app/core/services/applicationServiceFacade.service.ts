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

import { Injectable } from '@angular/core';
import { RequestMethod } from '@angular/http';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { Dictionary } from '../collections';

@Injectable()
export class ApplicationServiceFacade {

  private static readonly LOGIN = 'login';
  private static readonly LOGOUT = 'logout';
  private static readonly AUTHORIZE = 'authorize';
  private static readonly OAUTH = 'oauth';
  private static readonly ACCESS_KEY = 'access_key';
  private static readonly ACTIVE_LIST = 'active_list';
  private static readonly FULL_ACTIVE_LIST = 'full_active_list';
  private static readonly ENV = 'environment';
  private static readonly ACCESS_KEY_GENERATE = 'access_key_generate';
  private static readonly PROVISIONED_RESOURCES = 'provisioned_resources';
  private static readonly EXPLORATORY_ENVIRONMENT = 'exploratory_environment';
  private static readonly IMAGE = 'image';
  private static readonly SCHEDULER = 'scheduler';
  private static readonly TEMPLATES = 'templates';
  private static readonly COMPUTATIONAL_RESOURCES_TEMLATES = 'computational_templates';
  private static readonly COMPUTATIONAL_RESOURCES = 'computational_resources';
  private static readonly COMPUTATIONAL_RESOURCES_DATAENGINE = 'computational_resources_dataengine';
  private static readonly COMPUTATIONAL_RESOURCES_DATAENGINESERVICE = 'computational_resources_dataengineservice';
  private static readonly USER_PREFERENCES = 'user_preferences';
  private static readonly BUDGET = 'budget';
  private static readonly ENVIRONMENT_HEALTH_STATUS = 'environment_health_status';
  private static readonly META_DATA = 'meta';
  private static readonly ROLES = 'roles';
  private static readonly GROUPS = 'groups';
  private static readonly GROUP_ROLE = 'group_role';
  private static readonly GROUP_USER = 'group_user';
  private static readonly BACKUP = 'backup';
  private static readonly EDGE_NODE_START = 'edge_node_start';
  private static readonly EDGE_NODE_STOP = 'edge_node_stop';
  private static readonly EDGE_NODE_RECREATE = 'edge_node_recreate';
  private static readonly SNN_MONITOR = 'ssn_monitor';
  private static readonly LIB_GROUPS = 'lib_groups';
  private static readonly LIB_LIST = 'lib_list';
  private static readonly LIB_INSTALL = 'lib_install';
  private static readonly INSTALLED_LIBS_FORMAT = 'installed_libs_format';
  private static readonly INSTALLED_LIBS = 'installed_libs';
  private static readonly GIT_CREDS = 'git_creds';
  private static readonly BILLING = 'billing';
  private static readonly DOWNLOAD_REPORT = 'download_report';
  private static readonly SETTINGS = 'settings';
  private static readonly PROJECT = 'project';
  private static readonly USER_PROJECT = 'project/me';
  private static readonly ENDPOINT = 'endpoint';
  private accessTokenKey: string = 'access_token';
  private requestRegistry: Dictionary<string>;

  constructor(private http: HttpClient) {
    this.setupRegistry();
  }

  public buildLoginRequest(body: any): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LOGIN),
      body,
      { responseType: 'text', observe: 'response' });
  }

  public buildLogoutRequest(): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LOGOUT),
      '',
      { observe: 'response' });
  }

  public buildAuthorizeRequest(body: any): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.AUTHORIZE),
      body,
      {
        responseType: 'text',
        headers: { 'Content-Type': 'text/plain' },
        observe: 'response'
      });
  }

  public buildLocationCheck(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.OAUTH),
      null,
      { observe: 'response' });
  }

  public buildGetAuthToken(body: any): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.OAUTH) + body,
      null,
      { observe: 'response' });
  }

  public buildCheckUserAccessKeyRequest(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY),
      null,
      { observe: 'response' });
  }

  public buildGenerateAccessKey(): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY_GENERATE),
      null,
      { observe: 'response', responseType: 'text' });
  }

  public buildRegenerateAccessKey(option): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY_GENERATE) + option,
      null,
      { observe: 'response', responseType: 'text' });
  }

  public buildUploadUserAccessKeyRequest(body: any): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY),
      body,
      {
        observe: 'response',
        headers: { 'Upload': 'true' }
      });
  }

  public buildReuploadUserAccessKeyRequest(body: any, option: string): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY) + option,
      body,
      {
        observe: 'response',
        headers: { 'Upload': 'true' }
      });
  }

  public buildGetUserProvisionedResourcesRequest(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.PROVISIONED_RESOURCES),
      null);
  }

  public buildGetTemplatesRequest(params): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.TEMPLATES) + params,
      null);
  }

  public buildCreateExploratoryEnvironmentRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT),
      data,
      { responseType: 'text', observe: 'response' });
  }

  public buildRunExploratoryEnvironmentRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT),
      data,
      { responseType: 'text', observe: 'response' });
  }

  public buildSuspendExploratoryEnvironmentRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT),
      data, { responseType: 'text', observe: 'response' });
  }

  public buildCreateComputationalResources_DataengineServiceRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_DATAENGINESERVICE),
      data,
      { observe: 'response' });
  }

  public buildCreateComputationalResources_DataengineRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_DATAENGINE),
      data,
      { observe: 'response' });
  }

  public buildDeleteComputationalResourcesRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES),
      data);
  }

  public buildStopSparkClusterAction(data): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES),
      data);
  }

  public buildStartSparkClusterAction(params): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES) + params,
      null);
  }

  public buildGetUserPreferences(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.USER_PREFERENCES),
      null);
  }

  public buildUpdateUserPreferences(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.USER_PREFERENCES),
      data);
  }

  public buildGetEnvironmentHealthStatus(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS),
      null,
      { observe: 'response' });
  }

  public buildGetEnvironmentStatuses(data): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS),
      data);
  }

  public buildRunEdgeNodeRequest(): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EDGE_NODE_START),
      null,
      { responseType: 'text' });
  }

  public buildSuspendEdgeNodeRequest(): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EDGE_NODE_STOP),
      null,
      { responseType: 'text', observe: 'response' });
  }

  public buildRecreateEdgeNodeRequest(): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.EDGE_NODE_RECREATE),
      null,
      { responseType: 'text' });
  }

  public buildGetGroupsList(data): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_GROUPS),
      data);
  }

  public buildGetAvailableLibrariesList(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_LIST),
      data);
  }

  public buildGetAvailableDependenciest(params): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_LIST) + params,
      null);
  }

  public buildInstallLibraries(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.LIB_INSTALL),
      data,
      { observe: 'response', responseType: 'text' });
  }

  public buildGetInstalledLibrariesList(data): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.INSTALLED_LIBS_FORMAT),
      data);
  }

  public buildGetInstalledLibsByResource(data): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.INSTALLED_LIBS),
      data);
  }

  public buildGetGitCreds(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.GIT_CREDS),
      null);
  }

  public buildUpdateGitCredentials(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.GIT_CREDS),
      data);
  }

  public buildGetGeneralBillingData(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.BILLING),
      data);
  }

  public buildDownloadReportData(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.DOWNLOAD_REPORT),
      data,
      { observe: 'response', responseType: 'text' });
  }

  public buildCreateBackupRequest(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.BACKUP),
      data,
      { responseType: 'text', observe: 'response' });
  }
  public buildGetBackupStatusRequest(uuid): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.BACKUP),
      uuid);
  }

  public buildGetUserImages(image): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.IMAGE),
      image);
  }

  public buildGetImagesList(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.IMAGE),
      null);
  }

  public buildCreateAMI(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.IMAGE),
      data,
      { observe: 'response', responseType: 'text' });
  }

  public buildGetExploratorySchedule(data): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.SCHEDULER),
      data);
  }

  public buildSetExploratorySchedule(param, data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.SCHEDULER) + param,
      data,
      { observe: 'response' });
  }

  public buildResetScheduleSettings(data): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.SCHEDULER),
      data);
  }

  public BuildGetActiveSchcedulersData(param): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.SCHEDULER) + param,
      null);
  }

  public buildGetActiveUsers(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ACTIVE_LIST),
      null);
  }

  public buildManageEnvironment(action, data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ENV) + action,
      data,
      {
        observe: 'response',
        headers: { 'Content-Type': 'text/plain' }
      });
  }

  public buildGetAllEnvironmentData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.FULL_ACTIVE_LIST),
      null);
  }

  public buildEnvironmentManagement(param, data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ENV) + param,
      data,
      {
        observe: 'response',
        headers: { 'Content-Type': 'text/plain' }
      });
  }

  public buildGetSsnMonitorData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.SNN_MONITOR),
      null);
  }

  public buildGetTotalBudgetData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.SETTINGS),
      null);
  }

  public buildUpdateTotalBudgetData(param, method: number): Observable<any> {

    return this.buildRequest(method,
      this.requestRegistry.Item(ApplicationServiceFacade.SETTINGS) + param,
      null,
      { observe: 'response' });
  }

  public buildGetGroupsData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUPS),
      null);
  }

  public buildGetRolesData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ROLES),
      null);
  }

  public buildSetupNewGroup(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUPS),
      data);
  }

  public buildUpdateGroupData(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUPS),
      data);
  }

  public buildSetupRolesForGroup(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUP_ROLE),
      data);
  }

  public buildSetupUsersForGroup(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUP_USER),
      data);
  }

  public buildRemoveUsersForGroup(data): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUP_USER),
      data);
  }

  public buildRemoveGroupById(data): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.GROUPS),
      data);
  }

  public buildGetClusterConfiguration(param): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES) + param,
      null);
  }

  public buildEditClusterConfiguration(param, data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES) + param,
      data);
  }

  public buildGetExploratorySparkConfiguration(param): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT) + param,
      null);
  }

  public buildEditExploratorySparkConfiguration(param, data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT) + param,
      data);
  }

  public buildGetAppMetaData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.META_DATA),
      null);
  }

  public buildCreateProject(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.PROJECT),
      data);
  }

  public buildUpdateProject(data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.PROJECT),
      data);
  }

  public buildGetProjectsList(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.PROJECT),
      null);
  }

  public buildGetUserProjectsList(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.USER_PROJECT),
      null);
  }

  public buildDeleteProject(param): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.PROJECT) + param,
      null);
  }

  public buildToggleProjectStatus(param, data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.PROJECT) + param,
      data);
  }

  public buildUpdateProjectsBudget(param, data): Observable<any> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.PROJECT) + param,
      data,
      { observe: 'response' });
  }

  public buildGetEndpointsData(): Observable<any> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ENDPOINT),
      null);
  }

  public buildCreateEndpoint(data): Observable<any> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.ENDPOINT),
      data);
  }

  public buildDeleteEndpoint(param): Observable<any> {
    return this.buildRequest(RequestMethod.Delete,
      this.requestRegistry.Item(ApplicationServiceFacade.ENDPOINT) + param,
      null);
  }

  private setupRegistry(): void {
    this.requestRegistry = new Dictionary<string>();

    // Security
    this.requestRegistry.Add(ApplicationServiceFacade.LOGIN, '/api/user/login');
    this.requestRegistry.Add(ApplicationServiceFacade.LOGOUT, '/api/user/logout');
    this.requestRegistry.Add(ApplicationServiceFacade.AUTHORIZE, '/api/oauth/authorize');
    this.requestRegistry.Add(ApplicationServiceFacade.ACTIVE_LIST, '/api/environment/user');
    this.requestRegistry.Add(ApplicationServiceFacade.FULL_ACTIVE_LIST, '/api/environment/all');
    this.requestRegistry.Add(ApplicationServiceFacade.ENV, '/api/environment');

    this.requestRegistry.Add(ApplicationServiceFacade.OAUTH, '/api/oauth');
    this.requestRegistry.Add(ApplicationServiceFacade.ACCESS_KEY, '/api/user/access_key');
    this.requestRegistry.Add(ApplicationServiceFacade.ACCESS_KEY_GENERATE, '/api/user/access_key/generate');

    // Exploratory Environment
    this.requestRegistry.Add(ApplicationServiceFacade.PROVISIONED_RESOURCES,
      '/api/infrastructure/info');
    this.requestRegistry.Add(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT,
      '/api/infrastructure_provision/exploratory_environment');
    this.requestRegistry.Add(ApplicationServiceFacade.TEMPLATES,
      '/api/infrastructure_templates');
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
    this.requestRegistry.Add(ApplicationServiceFacade.BUDGET, '/api/user/settings/budget');

    // Environment Health Status
    this.requestRegistry.Add(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS, '/api/infrastructure/status');
    this.requestRegistry.Add(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS, '/api/infrastructure/status');
    this.requestRegistry.Add(ApplicationServiceFacade.META_DATA, '/api/infrastructure/meta');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_START, '/api/infrastructure/edge/start');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_STOP, '/api/infrastructure/edge/stop');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_RECREATE, '/api/user/access_key/recover');
    this.requestRegistry.Add(ApplicationServiceFacade.BACKUP, '/api/infrastructure/backup');
    this.requestRegistry.Add(ApplicationServiceFacade.SNN_MONITOR, '/api/sysinfo');
    this.requestRegistry.Add(ApplicationServiceFacade.ROLES, '/api/role');
    this.requestRegistry.Add(ApplicationServiceFacade.GROUPS, '/api/group');
    this.requestRegistry.Add(ApplicationServiceFacade.GROUP_ROLE, 'api/group/role');
    this.requestRegistry.Add(ApplicationServiceFacade.GROUP_USER, '/api/group/user');
    this.requestRegistry.Add(ApplicationServiceFacade.SETTINGS, '/api/settings');

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

    // project
    this.requestRegistry.Add(ApplicationServiceFacade.PROJECT, '/api/project');
    this.requestRegistry.Add(ApplicationServiceFacade.ENDPOINT, '/api/endpoint');
    this.requestRegistry.Add(ApplicationServiceFacade.USER_PROJECT, '/api/project/me');
  }

  private buildRequest(method: RequestMethod, url: string, body: any, opt?) {
    if (method === RequestMethod.Post) {
      return this.http.post(url, body, opt);
    } else if (method === RequestMethod.Delete) {
      return this.http.delete(body ? url + JSON.parse(body) : url, opt);
    } else if (method === RequestMethod.Put) {
      return this.http.put(url, body, opt);
    } else return this.http.get(body ? (url + body) : url, opt);
  }
}
