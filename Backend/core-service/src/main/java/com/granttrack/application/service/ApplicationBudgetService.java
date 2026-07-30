package com.granttrack.application.service;

import com.granttrack.application.dto.request.ApplicationBudgetRequest;
import com.granttrack.application.dto.response.ApplicationBudgetResponse;

import java.util.List;

public interface ApplicationBudgetService {
    ApplicationBudgetResponse add(Long applicationId, ApplicationBudgetRequest request);
    List<ApplicationBudgetResponse> listByApplication(Long applicationId);
    void remove(Long applicationId, Long id);
}
