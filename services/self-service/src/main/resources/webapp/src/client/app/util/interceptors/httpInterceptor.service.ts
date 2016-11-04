import {ConnectionBackend, RequestOptions, Http, Request, RequestOptionsArgs, Response, Headers} from "@angular/http";
import {Router} from "@angular/router";
import {Observable} from "rxjs";
import {AppRoutingService} from "../../routing/appRouting.service";
import {ApplicationSecurityService} from "../../services/applicationSecurity.service";

export class HttpInterceptor extends Http {
  constructor(backend: ConnectionBackend,
              defaultOptions: RequestOptions,
              private router: Router) {
    super(backend, defaultOptions);
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.request(url, options));
  }

  get(url: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.get(url,options));
  }

  post(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.post(url, body, this.getRequestOptionArgs(options)));
  }

  put(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.put(url, body, this.getRequestOptionArgs(options)));
  }

  delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.delete(url, options));
  }

  getRequestOptionArgs(options?: RequestOptionsArgs) : RequestOptionsArgs {
    if (options == null) {
      options = new RequestOptions();
    }
    if (options.headers == null) {
      options.headers = new Headers();
    }
    return options;
  }

  intercept(observable: Observable<Response>): Observable<Response> {
    return observable.catch((err, source) => {
      if ((err.status  == 403 || err.status == 401) && !err.url.toString().endsWith("login")) {
        localStorage.removeItem('access_token');
        this.router.navigate(['/login']);
        return Observable.of(err);
      } else {
        return Observable.throw(err);
      }
    });
  }
}
