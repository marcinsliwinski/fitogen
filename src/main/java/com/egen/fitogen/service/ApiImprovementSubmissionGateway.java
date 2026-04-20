package com.egen.fitogen.service;

import com.egen.fitogen.dto.ImprovementSubmissionDraft;
import com.egen.fitogen.dto.ImprovementSubmissionResult;

public class ApiImprovementSubmissionGateway implements ImprovementSubmissionGateway {

    @Override
    public ImprovementSubmissionResult submit(ImprovementSubmissionDraft draft) {
        throw new IllegalStateException("Endpoint API zgłoszeń ulepszeń nie jest jeszcze skonfigurowany.");
    }
}
