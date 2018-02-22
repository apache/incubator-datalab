package com.epam.dlab.auth;

/**
 * Applies additional verification during user login
 * e.g. verify if user exist in some external system
 */
@FunctionalInterface
public interface UserVerificationService {

	void verify(String username, UserInfo userInfo);
}
