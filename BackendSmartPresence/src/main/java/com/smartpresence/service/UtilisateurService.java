package com.smartpresence.service;

import com.smartpresence.dto.request.UserUpdateRequest;
import com.smartpresence.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UtilisateurService {

    List<UserResponse> findAll();

    UserResponse findById(UUID id);

    UserResponse update(UUID id, UserUpdateRequest request);

    void deactivate(UUID id);

    void activate(UUID id);
}
