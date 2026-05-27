package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.HomeworkCreateRequest;
import com.schoolerp.school_erp.dto.HomeworkResponse;
import com.schoolerp.school_erp.dto.HomeworkSubmissionRequest;
import com.schoolerp.school_erp.entity.HomeworkSubmission;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface HomeworkService {
    HomeworkResponse createHomework(HomeworkCreateRequest request, UUID creatorUserId);
    HomeworkSubmission submitHomework(HomeworkSubmissionRequest request);
    HomeworkSubmission gradeSubmission(UUID submissionId, BigDecimal marks, String remarks, UUID teacherUserId);
    List<HomeworkResponse> getHomeworksForClass(UUID classId);
    List<HomeworkSubmission> getSubmissionsForHomework(UUID homeworkId);
}
