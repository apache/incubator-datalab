package com.epam.dlab.auth.dex;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DexConfiguration {
	private String url;
	private String clientId;
	private String clientSecret;
	private String redirectUri;
	private String authorizationPath;
	private String scope;
}
