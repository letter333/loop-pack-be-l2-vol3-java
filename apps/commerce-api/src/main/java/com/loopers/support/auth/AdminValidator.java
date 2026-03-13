package com.loopers.support.auth;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class AdminValidator {

    private static final String ADMIN_LDAP = "loopers.admin";

    public void validate(String ldap) {
        if (ldap == null || ldap.isEmpty() || !ADMIN_LDAP.equals(ldap)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }
    }
}