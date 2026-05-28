package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.ParentChildResponse;
import com.schoolerp.school_erp.entity.*;
import com.schoolerp.school_erp.filter.TenantContext;
import com.schoolerp.school_erp.repository.*;
import com.schoolerp.school_erp.service.impl.ParentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParentServiceImplTest {

    @InjectMocks
    private ParentServiceImpl parentService;

    @Mock
    private ParentStudentMappingRepository parentStudentMappingRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SectionRepository sectionRepository;

    private UUID schoolId;
    private User parent;
    private User student;
    private SchoolClass schoolClass;
    private Section section;
    private StudentProfile studentProfile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        schoolId = UUID.randomUUID();
        TenantContext.setCurrentTenant(schoolId);

        School school = School.builder().id(schoolId).name("Test School").build();

        parent = User.builder()
                .id(UUID.randomUUID())
                .school(school)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .build();

        student = User.builder()
                .id(UUID.randomUUID())
                .school(school)
                .fullName("Billy Doe")
                .email("billy@example.com")
                .build();

        schoolClass = SchoolClass.builder()
                .id(UUID.randomUUID())
                .name("Class 10")
                .build();

        section = Section.builder()
                .id(UUID.randomUUID())
                .name("Section A")
                .build();

        studentProfile = StudentProfile.builder()
                .id(UUID.randomUUID())
                .user(student)
                .admissionNumber("ADM001")
                .rollNumber("12")
                .classId(schoolClass.getId())
                .sectionId(section.getId())
                .build();
    }

    @Test
    void testGetChildren_Success() {
        ParentStudentMapping mapping = ParentStudentMapping.builder()
                .parent(parent)
                .student(student)
                .relationship("MOTHER")
                .build();

        when(parentStudentMappingRepository.findByParentId(parent.getId()))
                .thenReturn(List.of(mapping));

        when(studentProfileRepository.findByUserIdAndDeletedAtIsNull(student.getId()))
                .thenReturn(Optional.of(studentProfile));

        when(schoolClassRepository.findById(schoolClass.getId()))
                .thenReturn(Optional.of(schoolClass));

        when(sectionRepository.findById(section.getId()))
                .thenReturn(Optional.of(section));

        List<ParentChildResponse> children = parentService.getChildren(parent.getId());

        assertNotNull(children);
        assertEquals(1, children.size());
        ParentChildResponse child = children.get(0);
        assertEquals(student.getId(), child.getStudentId());
        assertEquals("Billy Doe", child.getFullName());
        assertEquals("jane@example.com", parent.getEmail());
        assertEquals("ADM001", child.getAdmissionNumber());
        assertEquals("Class 10", child.getClassName());
        assertEquals("Section A", child.getSectionName());
    }

    @Test
    void testGetChildren_TenantMismatch() {
        UUID otherSchoolId = UUID.randomUUID();
        School otherSchool = School.builder().id(otherSchoolId).build();
        User otherSchoolStudent = User.builder()
                .id(UUID.randomUUID())
                .school(otherSchool)
                .fullName("Other Student")
                .build();

        ParentStudentMapping mapping = ParentStudentMapping.builder()
                .parent(parent)
                .student(otherSchoolStudent)
                .relationship("MOTHER")
                .build();

        when(parentStudentMappingRepository.findByParentId(parent.getId()))
                .thenReturn(List.of(mapping));

        List<ParentChildResponse> children = parentService.getChildren(parent.getId());

        assertNotNull(children);
        assertTrue(children.isEmpty()); // Should skip the child because of tenant school mismatch
    }
}
