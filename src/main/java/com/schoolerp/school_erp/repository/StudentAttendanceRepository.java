package com.schoolerp.school_erp.repository;

import com.schoolerp.school_erp.entity.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {
    
    Optional<StudentAttendance> findByStudentIdAndDateAndSectionSubjectIdIsNullAndDeletedAtIsNull(
            UUID studentId, LocalDate date);

    Optional<StudentAttendance> findByStudentIdAndDateAndSectionSubjectIdAndDeletedAtIsNull(
            UUID studentId, LocalDate date, UUID sectionSubjectId);

    List<StudentAttendance> findBySchoolIdAndStudentClassIdAndDateAndDeletedAtIsNull(
            UUID schoolId, UUID classId, LocalDate date);
}
