package com.regisx001.dQul.authentication.service;

import com.regisx001.dQul.authentication.dto.AuthenticationRequest;
import com.regisx001.dQul.authentication.dto.AuthenticationResponse;
import com.regisx001.dQul.authentication.dto.RegisterRequest;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    boolean verifyToken(String token);
}
