package com.epam.dlab.auth.service;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.dex.DexConfiguration;
import com.epam.dlab.auth.model.DexTokenResponse;
import com.epam.dlab.auth.model.DexUser;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Singleton
@Slf4j
public class DexOauthServiceImpl implements DexOauthService {

	private static final String DEX_LOGIN_PAGE = "%s%s?&response_type=code&client_id=%s&client_secret=%s" +
			"&redirect_uri=%s&scope=%s&state=%s";
	private static final String TOKEN_URI_FORMAT = "%s/token?grant_type=authorization_code&code=%s&redirect_uri=%s";
	private static final String BASIC_AUTHORIZATION_FORMAT = "Basic %s";
	private static final String GRANT_TYPE = "grant_type";
	private static final String CODE = "code";
	private static final String REDIRECT_URI = "redirect_uri";
	private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
	private final DexConfiguration config;
	private final HttpClient client;
	private final ObjectMapper mapper;
	private final UserInfoDAO userInfoDAO;

	@Inject
	public DexOauthServiceImpl(SecurityServiceConfiguration configuration, HttpClient client, ObjectMapper mapper,
							   UserInfoDAO userInfoDAO) {
		this.config = configuration.getDexConfiguration();
		this.client = client;
		this.mapper = mapper;
		this.userInfoDAO = userInfoDAO;
	}

	@Override
	public String getDexOauthUrl() {
		final String dexUrl = config.getUrl();
		final String authorizationPath = config.getAuthorizationPath();
		final String clientId = config.getClientId();
		final String clientSecret = config.getClientSecret();
		final String redirectUri = config.getRedirectUri();
		final String scope = config.getScope();
		final String state = UUID.randomUUID().toString();
		return format(DEX_LOGIN_PAGE, dexUrl, authorizationPath, clientId, clientSecret, redirectUri, scope, state);
	}

	@Override
	public UserInfo getUserInfo(String code) {
		final DexTokenResponse tokenResponse = getToken(code);
		final UserInfo userInfo = toUserInfo(tokenResponse.getIdToken(), tokenResponse.getExprires());
		userInfoDAO.saveUserInfo(userInfo);
		return userInfo;
	}

	private UserInfo toUserInfo(String idToken, long expiresAfter) {
		try {
			final String tokenPayload = new String(Base64.decodeBase64(idToken.split("\\.")[1]));
			final DexUser dexUser = mapper.readValue(tokenPayload, DexUser.class);
			final UserInfo userInfo = new UserInfo(dexUser.getEmail(), idToken);
			if (dexUser.getGroups() != null) {
				userInfo.addRoles(dexUser.getGroups());
			}
			final ZonedDateTime expirationDateTime =
					LocalDateTime.now().plusSeconds(expiresAfter).atZone(ZoneId.systemDefault());
			userInfo.setExpireAt(Date.from(expirationDateTime.toInstant()));
			return userInfo;
		} catch (IOException e) {
			log.error("Can not extract user information due to: {}", e.getMessage());
			throw new DlabException("Can not extract user information due to: " + e.getMessage());
		}
	}

	private DexTokenResponse getToken(String code) {
		try {
			final String uri = format(TOKEN_URI_FORMAT, config.getUrl(), code, config.getRedirectUri());
			final HttpPost tokenRequest = new HttpPost(uri);
			tokenRequest.addHeader(HttpHeaders.AUTHORIZATION, format(BASIC_AUTHORIZATION_FORMAT, encodedAuthString()));
			tokenRequest.setEntity(getAccessTokenRequestBody(code));
			final HttpResponse tokenResponse = client.execute(tokenRequest);
			return mapper.readValue(tokenResponse.getEntity().getContent(), DexTokenResponse.class);
		} catch (IOException e) {
			log.error("Can not get access token due to: {}", e.getMessage());
			throw new DlabException("Can not get access token due to: " + e.getMessage());
		}
	}

	private UrlEncodedFormEntity getAccessTokenRequestBody(String code) {

		return new UrlEncodedFormEntity(asList(
				new BasicNameValuePair(GRANT_TYPE, AUTHORIZATION_CODE_GRANT_TYPE),
				new BasicNameValuePair(CODE, code),
				new BasicNameValuePair(REDIRECT_URI, config.getRedirectUri())
		), Consts.UTF_8);
	}

	private String encodedAuthString() {
		return Base64.encodeBase64String(String.join(":", config.getClientId(), config.getClientSecret()).getBytes());
	}
}
