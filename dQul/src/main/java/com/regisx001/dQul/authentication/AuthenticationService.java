package com.regisx001.dQul.services;

import com.regisx001.dQul.domain.dto.auth.AuthenticationRequest;
import com.regisx001.dQul.domain.dto.auth.AuthenticationResponse;
import com.regisx001.dQul.domain.dto.auth.RegisterRequest;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    boolean verifyToken(String token);
}
