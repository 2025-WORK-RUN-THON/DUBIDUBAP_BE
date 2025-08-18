package com.guineafigma.domain.user.service;

import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.dto.response.UserResponse;
import com.guineafigma.domain.user.entity.User;

public interface UserService {
    
    LoginResponse authenticateUser(LoginRequest request);
    
    void logoutUser(Long userId);
    
    UserResponse getUserById(Long userId);
    
    User findActiveUserById(Long userId);
}