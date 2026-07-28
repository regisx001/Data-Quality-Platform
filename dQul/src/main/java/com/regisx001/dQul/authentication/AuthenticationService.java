package com.regisx001.dQul.authentication;

import com.regisx001.dQul.authentication.AuthenticationRequest;
import com.regisx001.dQul.authentication.AuthenticationResponse;
import com.regisx001.dQul.authentication.RegisterRequest;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    boolean verifyToken(String token);
}
