package com.hospital.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hospital.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 签发与校验
 */
@Component
public class JwtUtil {

    @Value("${hospital.jwt.secret}")
    private String secret;

    @Value("${hospital.jwt.expire-seconds}")
    private long expireSeconds;

    public String createToken(Integer userId, String account, String role) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000L);
        return JWT.create()
                .withClaim(Constants.CLAIM_USER_ID, userId)
                .withClaim(Constants.CLAIM_ACCOUNT, account)
                .withClaim(Constants.CLAIM_ROLE, role)
                .withIssuedAt(now)
                .withExpiresAt(expireAt)
                .sign(Algorithm.HMAC256(secret));
    }

    /** 校验失败会抛 JWTVerificationException */
    public DecodedJWT verify(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
        return verifier.verify(token);
    }

    public Integer getUserId(String token) {
        return verify(token).getClaim(Constants.CLAIM_USER_ID).asInt();
    }

    public String getRole(String token) {
        return verify(token).getClaim(Constants.CLAIM_ROLE).asString();
    }

    public String getAccount(String token) {
        return verify(token).getClaim(Constants.CLAIM_ACCOUNT).asString();
    }
}
