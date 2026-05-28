package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.entity.*;
import com.schoolerp.school_erp.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ParentStudentMappingTest {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ParentStudentMappingRepository parentStudentMappingRepository;

    @Test
    void testClassSectionAndParentStudentMapping() {
        // 1. Create and persist School
        School school = School.builder()
                .name("Greenwood High")
                .subdomain("greenwood-high-test")
                .isActive(true)
                .build();
        school = schoolRepository.save(school);
        assertNotNull(school.getId());

        // 2. Create and persist SchoolClass
        SchoolClass schoolClass = SchoolClass.builder()
                .school(school)
                .name("Class 10")
                .build();
        schoolClass = schoolClassRepository.save(schoolClass);
        assertNotNull(schoolClass.getId());

        // 3. Create and persist Section
        Section section = Section.builder()
                .schoolClass(schoolClass)
                .name("Section A")
                .build();
        section = sectionRepository.save(section);
        assertNotNull(section.getId());

        // Verify Class & Section relationship
        List<Section> sections = sectionRepository.findBySchoolClassIdAndDeletedAtIsNull(schoolClass.getId());
        assertEquals(1, sections.size());
        assertEquals("Section A", sections.get(0).getName());

        // 4. Create parent and student Users
        User parent = User.builder()
                .school(school)
                .fullName("Jane Doe")
                .email("jane.doe@example.com")
                .passwordHash("hashed")
                .isActive(true)
                .build();
        parent = userRepository.save(parent);

        User student1 = User.builder()
                .school(school)
                .fullName("Billy Doe")
                .email("billy.doe@example.com")
                .passwordHash("hashed")
                .isActive(true)
                .build();
        student1 = userRepository.save(student1);

        User student2 = User.builder()
                .school(school)
                .fullName("Sarah Doe")
                .email("sarah.doe@example.com")
                .passwordHash("hashed")
                .isActive(true)
                .build();
        student2 = userRepository.save(student2);

        // 5. Create Parent-Student Mappings
        ParentStudentMapping mapping1 = ParentStudentMapping.builder()
                .parent(parent)
                .student(student1)
                .relationship("MOTHER")
                .build();
        parentStudentMappingRepository.save(mapping1);

        ParentStudentMapping mapping2 = ParentStudentMapping.builder()
                .parent(parent)
                .student(student2)
                .relationship("MOTHER")
                .build();
        parentStudentMappingRepository.save(mapping2);

        // 6. Query mappings
        List<ParentStudentMapping> parentMappings = parentStudentMappingRepository.findByParentId(parent.getId());
        assertEquals(2, parentMappings.size());

        // Verify that the parent can retrieve all their children
        boolean hasBilly = parentMappings.stream().anyMatch(m -> m.getStudent().getFullName().equals("Billy Doe"));
        boolean hasSarah = parentMappings.stream().anyMatch(m -> m.getStudent().getFullName().equals("Sarah Doe"));
        assertTrue(hasBilly);
        assertTrue(hasSarah);

        // Verify student mapping query
        List<ParentStudentMapping> studentMappings = parentStudentMappingRepository.findByStudentId(student1.getId());
        assertEquals(1, studentMappings.size());
        assertEquals("Jane Doe", studentMappings.get(0).getParent().getFullName());
    }
}
