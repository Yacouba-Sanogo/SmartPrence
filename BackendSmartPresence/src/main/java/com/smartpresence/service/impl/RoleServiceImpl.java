package com.smartpresence.service.impl;

import com.smartpresence.dto.response.RoleResponse;
import com.smartpresence.mapper.RoleMapper;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleMapper.toResponseList(roleRepository.findAll());
    }
}
