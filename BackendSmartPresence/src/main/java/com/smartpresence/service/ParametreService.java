package com.smartpresence.service;

import com.smartpresence.dto.request.ParametreEtablissementRequest;
import com.smartpresence.dto.response.ParametreEtablissementResponse;

public interface ParametreService {

    ParametreEtablissementResponse get();

    ParametreEtablissementResponse update(ParametreEtablissementRequest request);
}
