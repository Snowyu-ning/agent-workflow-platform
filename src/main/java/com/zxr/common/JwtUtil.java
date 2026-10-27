package com.zxr.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component // 交给 Spring 管理
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretString;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes());
    }

    public String createToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Long parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}