package com.schoolerp.school_erp.service.impl;

import com.schoolerp.school_erp.dto.ExamCreateRequest;
import com.schoolerp.school_erp.dto.ExamMarkRequest;
import com.schoolerp.school_erp.dto.ExamMarkResponse;
import com.schoolerp.school_erp.dto.GradebookResponse;
import com.schoolerp.school_erp.entity.*;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.*;
import com.schoolerp.school_erp.service.ExamService;
import com.schoolerp.school_erp.strategy.NotificationFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamTypeRepository examTypeRepository;

    @Autowired
    private ExamMarkRepository examMarkRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private NotificationFactory notificationFactory;

    @Override
    @Transactional
    public ExamMarkResponse createExam(ExamCreateRequest request, UUID creatorUserId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        ExamType examType = examTypeRepository.findByIdAndSchoolIdAndDeletedAtIsNull(request.getExamTypeId(), schoolId)
                .orElseThrow(() -> new IllegalArgumentException("ExamType not found"));

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Exam exam = Exam.builder()
                .school(school)
                .examType(examType)
                .classId(request.getClassId())
                .sectionId(request.getSectionId())
                .subjectId(request.getSubjectId())
                .examDate(request.getExamDate())
                .maxMarks(request.getMaxMarks())
                .createdBy(creator)
                .build();

        exam = examRepository.save(exam);

        return ExamMarkResponse.builder()
                .id(exam.getId())
                .examId(exam.getId())
                .examName(examType.getName() + " - " + request.getSubjectId())
                .maxMarks(exam.getMaxMarks())
                .build();
    }

    @Override
    @Transactional
    public ExamMarkResponse enterMarks(ExamMarkRequest request) {
        UUID schoolId = TenantContext.getCurrentTenant();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        Exam exam = examRepository.findByIdAndSchoolIdAndDeletedAtIsNull(request.getExamId(), schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        StudentProfile student = studentProfileRepository.findById(request.getStudentProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (!student.getUser().getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to student profile.");
        }

        BigDecimal marksObtained = request.getMarksObtained();
        BigDecimal maxMarks = exam.getMaxMarks();
        if (marksObtained.compareTo(maxMarks) > 0) {
            throw new IllegalArgumentException("Marks obtained cannot exceed max marks: " + maxMarks);
        }

        BigDecimal percentage = marksObtained.divide(maxMarks, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        String grade = determineGrade(percentage);

        // Check if marks already inputted for this exam and student
        ExamMark mark = examMarkRepository.findByExamIdAndStudentIdAndDeletedAtIsNull(exam.getId(), student.getId())
                .orElse(null);

        if (mark == null) {
            mark = ExamMark.builder()
                    .school(school)
                    .exam(exam)
                    .student(student)
                    .marksObtained(marksObtained)
                    .percentage(percentage)
                    .grade(grade)
                    .remarks(request.getRemarks())
                    .build();
        } else {
            mark.setMarksObtained(marksObtained);
            mark.setPercentage(percentage);
            mark.setGrade(grade);
            mark.setRemarks(request.getRemarks());
        }

        mark = examMarkRepository.save(mark);

        // Dispatch simulated notification for grade alert
        try {
            String recipient = student.getUser().getEmail();
            String msg = "Grade Alert: You scored " + marksObtained + "/" + maxMarks + 
                " (" + percentage.setScale(2, RoundingMode.HALF_UP) + "%) in exam " + 
                exam.getExamType().getName() + " - " + exam.getSubjectId() + ". Grade: " + grade;
            notificationFactory.getService("whatsapp").sendNotice(recipient, msg);
        } catch (Exception e) {
            // Log warning but don't fail enterMarks
            org.slf4j.LoggerFactory.getLogger(ExamServiceImpl.class)
                .warn("Failed to dispatch WhatsApp grade alert: {}", e.getMessage());
        }

        return mapToMarkResponse(mark);
    }

    @Override
    @Transactional(readOnly = true)
    public GradebookResponse getGradebook(UUID studentProfileId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        StudentProfile student = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (!student.getUser().getSchool().getId().equals(schoolId)) {
            throw new SecurityException("Unauthorized tenant access to student profile.");
        }

        List<ExamMark> marks = examMarkRepository.findByStudentIdAndSchoolIdAndDeletedAtIsNull(studentProfileId, schoolId);

        List<ExamMarkResponse> marksResponses = marks.stream()
                .map(this::mapToMarkResponse)
                .collect(Collectors.toList());

        BigDecimal aggregatePercentage = BigDecimal.ZERO;
        String overallGrade = "N/A";

        if (!marks.isEmpty()) {
            BigDecimal totalPercentage = BigDecimal.ZERO;
            for (ExamMark mark : marks) {
                totalPercentage = totalPercentage.add(mark.getPercentage());
            }
            aggregatePercentage = totalPercentage.divide(BigDecimal.valueOf(marks.size()), 2, RoundingMode.HALF_UP);
            overallGrade = determineGrade(aggregatePercentage);
        }

        return GradebookResponse.builder()
                .studentProfileId(student.getId())
                .studentName(student.getUser().getEmail()) // using email as identifier name representation
                .admissionNumber(student.getAdmissionNumber())
                .classId(student.getClassId() != null ? student.getClassId().toString() : null)
                .sectionId(student.getSectionId() != null ? student.getSectionId().toString() : null)
                .marks(marksResponses)
                .aggregatePercentage(aggregatePercentage)
                .overallGrade(overallGrade)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamMarkResponse> getExamMarks(UUID examId) {
        UUID schoolId = TenantContext.getCurrentTenant();
        List<ExamMark> marks = examMarkRepository.findByExamIdAndSchoolIdAndDeletedAtIsNull(examId, schoolId);
        return marks.stream().map(this::mapToMarkResponse).collect(Collectors.toList());
    }

    private String determineGrade(BigDecimal percentage) {
        double pct = percentage.doubleValue();
        if (pct >= 90.0) return "A+";
        if (pct >= 80.0) return "A";
        if (pct >= 70.0) return "B";
        if (pct >= 60.0) return "C";
        if (pct >= 50.0) return "D";
        return "F";
    }

    private ExamMarkResponse mapToMarkResponse(ExamMark mark) {
        return ExamMarkResponse.builder()
                .id(mark.getId())
                .examId(mark.getExam().getId())
                .examName(mark.getExam().getExamType().getName() + " - " + mark.getExam().getSubjectId())
                .studentProfileId(mark.getStudent().getId())
                .studentName(mark.getStudent().getUser().getEmail())
                .marksObtained(mark.getMarksObtained())
                .maxMarks(mark.getExam().getMaxMarks())
                .percentage(mark.getPercentage().setScale(2, RoundingMode.HALF_UP))
                .grade(mark.getGrade())
                .remarks(mark.getRemarks())
                .build();
    }
}
