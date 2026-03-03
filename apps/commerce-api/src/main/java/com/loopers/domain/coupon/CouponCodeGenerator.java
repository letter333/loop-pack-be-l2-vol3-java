package com.loopers.domain.coupon;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CouponCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 12;
    private static final int GROUP_SIZE = 4;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i > 0 && i % GROUP_SIZE == 0) {
                code.append("-");
            }
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }
}
