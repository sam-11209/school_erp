package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.HomeworkCreateRequest;
import com.schoolerp.school_erp.dto.HomeworkResponse;
import com.schoolerp.school_erp.dto.HomeworkSubmissionRequest;
import com.schoolerp.school_erp.entity.*;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.*;
import com.schoolerp.school_erp.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HomeworkServiceImpl implements HomeworkService {

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private HomeworkSubmissionRepository homeworkSubmissionRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Override
    @Transactional
    public HomeworkResponse createHomework(HomeworkCreateRequest request, UUID creatorUserId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Homework homework = Homework.builder()
                .school(school)
                .classId(request.getClassId())
                .sectionId(request.getSectionId())
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .fileAttachmentPath(request.getFileAttachmentPath())
                .createdBy(creator)
                .build();

        homework = homeworkRepository.save(homework);

        return mapToResponse(homework);
    }

    @Override
    @Transactional
    public HomeworkSubmission submitHomework(HomeworkSubmissionRequest request) {
        UUID schoolId = TenantContext.getCurrentTenant();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        Homework homework = homeworkRepository.findByIdAndSchoolIdAndDeletedAtIsNull(request.getHomeworkId(), schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Homework not found"));

        StudentProfile student = studentProfileRepository.findById(request.getStudentProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (!student.getUser().getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to student profile.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String status = now.isAfter(homework.getDueDate()) ? "LATE" : "SUBMITTED";

        HomeworkSubmission submission = homeworkSubmissionRepository
                .findByHomeworkIdAndStudentIdAndDeletedAtIsNull(homework.getId(), student.getId())
                .orElse(null);

        if (submission == null) {
            submission = HomeworkSubmission.builder()
                    .school(school)
                    .homework(homework)
                    .student(student)
                    .submittedAt(now)
                    .filePath(request.getFilePath())
                    .status(status)
                    .build();
        } else {
            submission.setSubmittedAt(now);
            submission.setFilePath(request.getFilePath());
            submission.setStatus(status);
        }

        return homeworkSubmissionRepository.save(submission);
    }

    @Override
    @Transactional
    public HomeworkSubmission gradeSubmission(UUID submissionId, BigDecimal marks, String remarks, UUID teacherUserId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        HomeworkSubmission submission = homeworkSubmissionRepository
                .findByIdAndSchoolIdAndDeletedAtIsNull(submissionId, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        User teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        submission.setMarksObtained(marks);
        submission.setRemarks(remarks);
        submission.setStatus("GRADED");
        submission.setGradedBy(teacher);

        return homeworkSubmissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomeworksForClass(UUID classId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        List<Homework> homeworks = homeworkRepository.findBySchoolIdAndClassIdAndDeletedAtIsNull(schoolId, classId);
        return homeworks.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkSubmission> getSubmissionsForHomework(UUID homeworkId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        return homeworkSubmissionRepository.findByHomeworkIdAndSchoolIdAndDeletedAtIsNull(homeworkId, schoolId);
    }

    private HomeworkResponse mapToResponse(Homework homework) {
        return HomeworkResponse.builder()
                .id(homework.getId())
                .classId(homework.getClassId())
                .sectionId(homework.getSectionId())
                .subjectId(homework.getSubjectId())
                .title(homework.getTitle())
                .description(homework.getDescription())
                .dueDate(homework.getDueDate())
                .fileAttachmentPath(homework.getFileAttachmentPath())
                .createdByUserId(homework.getCreatedBy().getId())
                .createdAt(homework.getCreatedAt())
                .build();
    }
}
