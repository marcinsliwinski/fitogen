package com.egen.fitogen.service;

import com.egen.fitogen.dto.ImprovementSubmissionDraft;
import com.egen.fitogen.dto.ImprovementSubmissionResult;

public interface ImprovementSubmissionGateway {

    ImprovementSubmissionResult submit(ImprovementSubmissionDraft draft) throws Exception;
}
