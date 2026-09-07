package com.smartpresence.service;

import com.smartpresence.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> findAll();
}
