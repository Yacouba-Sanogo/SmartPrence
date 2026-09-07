package com.smartpresence.service;

import com.smartpresence.dto.request.SalleRequest;
import com.smartpresence.dto.response.SalleResponse;

import java.util.List;

public interface SalleService {

    List<SalleResponse> findAll();

    SalleResponse findById(Long id);

    SalleResponse create(SalleRequest request);

    SalleResponse update(Long id, SalleRequest request);

    void delete(Long id);
}
