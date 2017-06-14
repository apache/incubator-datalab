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
  private static readonly ACCESS_KEY = 'access_key';
  private static readonly PROVISIONED_RESOURCES = 'provisioned_resources';
  private static readonly EXPLORATORY_ENVIRONMENT = 'exploratory_environment';
  private static readonly EXPLORATORY_ENVIRONMENT_TEMPLATES = 'exploratory_environment_templates';
  private static readonly COMPUTATIONAL_RESOURCES_TEMLATES = 'computational_resources_templates';
  private static readonly COMPUTATIONAL_RESOURCES = 'computational_resources';
  private static readonly COMPUTATIONAL_RESOURCES_CONFIGURATION = 'computational_resources_configuration';
  private static readonly USER_PREFERENCES = 'user_preferences';
  private static readonly ENVIRONMENT_HEALTH_STATUS = 'environment_health_status';
  private static readonly EDGE_NODE_START = 'edge_node_start';
  private static readonly EDGE_NODE_STOP = 'edge_node_stop';
  private static readonly EDGE_NODE_RECREATE = 'edge_node_recreate';
  private static readonly LIB_GROUPS = 'lib_groups';
  private static readonly LIB_LIST = 'lib_list';
  private static readonly LIB_INSTALL = 'lib_install';
  private static readonly INSTALLED_LIBS = 'installed_libs';
  private static readonly BILLING = 'billing';
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

  public buildCheckUserAccessKeyRequest(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.ACCESS_KEY),
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

  public buildGetComputationalResourcesConfiguration(): Observable<Response> {
    return this.buildRequest(RequestMethod.Get,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_CONFIGURATION),
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

  public buildCreateComputationalResourcesRequest(data): Observable<Response> {
    return this.buildRequest(RequestMethod.Put,
      this.requestRegistry.Item(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES),
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
    return this.buildRequest(RequestMethod.Post,
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
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.INSTALLED_LIBS),
      data,
      this.getRequestOptions(true, true));
  }

  public buildGetGeneralBillingData(): Observable<Response> {
    return this.buildRequest(RequestMethod.Post,
      this.requestRegistry.Item(ApplicationServiceFacade.INSTALLED_LIBS),
      null,
      this.getRequestOptions(true, true));
  }

  private setupRegistry(): void {
    this.requestRegistry = new Dictionary<string>();

    // Security
    this.requestRegistry.Add(ApplicationServiceFacade.LOGIN, '/api/user/login');
    this.requestRegistry.Add(ApplicationServiceFacade.LOGOUT, '/api/user/logout');
    this.requestRegistry.Add(ApplicationServiceFacade.AUTHORIZE, '/api/user/authorize');
    this.requestRegistry.Add(ApplicationServiceFacade.ACCESS_KEY, '/api/user/access_key');

    // Exploratory Environment
    this.requestRegistry.Add(ApplicationServiceFacade.PROVISIONED_RESOURCES,
      '/api/infrastructure_provision/provisioned_user_resources');
    this.requestRegistry.Add(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT,
      '/api/infrastructure_provision/exploratory_environment');
    this.requestRegistry.Add(ApplicationServiceFacade.EXPLORATORY_ENVIRONMENT_TEMPLATES,
      '/api/infrastructure_provision/exploratory_environment_templates');


    // Computational Resources
    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES,
      '/api/infrastructure_provision/computational_resources');
    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_TEMLATES,
      '/api/infrastructure_provision/computational_resources_templates');
    this.requestRegistry.Add(ApplicationServiceFacade.COMPUTATIONAL_RESOURCES_CONFIGURATION,
      '/api/infrastructure_provision/computational_resources/configuration');

    // Filtering Configuration
    this.requestRegistry.Add(ApplicationServiceFacade.USER_PREFERENCES, '/api/user/settings');

    // Environment Health Status
    this.requestRegistry.Add(ApplicationServiceFacade.ENVIRONMENT_HEALTH_STATUS, '/api/infrastructure/status');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_START, '/api/infrastructure/edge/start');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_STOP, '/api/infrastructure/edge/stop');
    this.requestRegistry.Add(ApplicationServiceFacade.EDGE_NODE_RECREATE, '/api/user/access_key/recover');

    // Libraries Installation
    this.requestRegistry.Add(ApplicationServiceFacade.LIB_GROUPS, '/api/infrastructure_provision/lib_groups');
    this.requestRegistry.Add(ApplicationServiceFacade.LIB_LIST, '/api/infrastructure_provision/lib_list');
    this.requestRegistry.Add(ApplicationServiceFacade.LIB_INSTALL, '/api/infrastructure_provision/exploratory_environment/lib_install');
    this.requestRegistry.Add(ApplicationServiceFacade.INSTALLED_LIBS, '/api/infrastructure_provision/exploratory_environment/lib_list');

    // billing report
    this.requestRegistry.Add(ApplicationServiceFacade.BILLING, '/api/infrastructure_provision/billing');
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
