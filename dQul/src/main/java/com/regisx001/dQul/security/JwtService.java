package com.regisx001.dQul.services;

import java.util.List;

import com.regisx001.dQul.domain.entities.User;

public interface JwtService {
    public String extractUsername(String token);

    public List<String> extractRoles(String token);

    public String generateToken(User user);

    public boolean isTokenExpired(String token);

    public boolean isTokenValid(String token, User user);

    public long getJwtExpiration();

}