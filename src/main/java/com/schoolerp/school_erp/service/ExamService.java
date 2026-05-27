package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.ExamCreateRequest;
import com.schoolerp.school_erp.dto.ExamMarkRequest;
import com.schoolerp.school_erp.dto.ExamMarkResponse;
import com.schoolerp.school_erp.dto.GradebookResponse;

import java.util.List;
import java.util.UUID;

public interface ExamService {
    ExamMarkResponse createExam(ExamCreateRequest request, UUID creatorUserId);
    ExamMarkResponse enterMarks(ExamMarkRequest request);
    GradebookResponse getGradebook(UUID studentProfileId);
    List<ExamMarkResponse> getExamMarks(UUID examId);
}
