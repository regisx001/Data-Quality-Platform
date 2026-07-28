package com.regisx001.dQul.services.impl;

import java.util.List;

import com.regisx001.dQul.domain.entities.User;
import com.regisx001.dQul.services.JwtService;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${security.jwt.key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    @Override
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @SuppressWarnings("deprecation")
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

    }

    @Override
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(user.getRole()));

        return buildToken(claims, user, jwtExpiration);
    }

    private String buildToken(Map<String, Object> claims, User user, long jwtExpiration) {
        // DEPRECATED
        // return Jwts.builder()
        // .setClaims(extraClaims)
        // .setSubject(userDetails.getUsername())
        // .setIssuedAt(new Date(System.currentTimeMillis()))
        // .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
        // .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Fixed to HS256
        // .compact();

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey()) // Fixed to HS256
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public boolean isTokenValid(String token, User user) {
        try {
            final String username = extractUsername(token);
            return username.equals(user.getUsername()) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false;
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }

    private Date extractExpiration(String token) { // Fixed method name
        return extractClaims(token, Claims::getExpiration);
    }

    @Override
    public long getJwtExpiration() {
        return this.jwtExpiration;
    }

    @Override
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return ((List<?>) claims.get("roles")).stream()
                .map(Object::toString)
                .toList();
    }
}