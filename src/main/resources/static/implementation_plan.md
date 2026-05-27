# School ERP Expansion Implementation Plan

This implementation plan covers building out the remaining modules of the multi-tenant School ERP system:
1. **Exams & Grading (Gradebook)**
2. **Homework & Submissions System**
3. **Payment Gateways & Orders**
4. **Board Announcements & Alerts**

It also details creating a SQL schema script file (`schema.sql`) to define all 20 database tables for standalone setup and manual testing.

---

## User Review Required

> [!IMPORTANT]
> 1. **Authentication Mode**: Security runs through interceptors (`TenantFilter`, `RateLimitFilter`) parsing HTTP headers like `X-Tenant-ID` and `X-User-ID`. We will map authorization dynamically based on the active User role.
> 2. **WhatsApp Notification Stubs**: Since integration credentials aren't provided, our notification dispatches (e.g., announcements or grade alerts) will log the target payload and generate a placeholder output URL (e.g., simulating a WhatsApp chat redirect trigger).
> 3. **SQL Queries Export**: All SQL table creation scripts will be written to `src/main/resources/static/schema.sql`.

---

## Proposed Changes

### Database DDL Schema Setup

#### [NEW] [schema.sql](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/resources/static/schema.sql)
A complete PostgreSQL DDL file creating 20 tables:
1. `schools`
2. `school_configurations`
3. `roles`
4. `permissions`
5. `role_permissions`
6. `users`
7. `user_roles`
8. `student_profiles`
9. `student_attendance`
10. `invoices`
11. `invoice_items`
12. `exam_types`
13. `exams`
14. `exam_marks`
15. `homeworks`
16. `homework_submissions`
17. `payment_gateways`
18. `payment_orders`
19. `announcements`
20. `notifications`

---

### Module 1: Exams & Grading (Gradebook)

#### [NEW] [ExamType.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/ExamType.java)
Entity defining Midterm, Quiz 1, Final Exam types, scoped per tenant.

#### [NEW] [Exam.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/Exam.java)
Entity for individual exams mapping to a class, section, subject, date, and max marks.

#### [NEW] [ExamMark.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/ExamMark.java)
Entity storing student grades, raw marks, percentage, and remarks.

#### [NEW] Repositories:
* `ExamTypeRepository.java`
* `ExamRepository.java`
* `ExamMarkRepository.java`

#### [NEW] DTOs under `com.schoolerp.school_erp.dto`:
* `ExamCreateRequest.java`
* `ExamMarkRequest.java`
* `ExamMarkResponse.java`
* `GradebookResponse.java`

#### [NEW] [ExamService.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/ExamService.java) & [ExamServiceImpl.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/impl/ExamServiceImpl.java)
Calculates percentages, matches ranges to grades (A+, B, etc.), and isolates CRUD by tenant boundary.

#### [NEW] [ExamController.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/controller/ExamController.java)
* `POST /api/exams/create` (Enforces role checks: Teacher/Principal/Admin)
* `POST /api/exams/marks` (Teacher inputs raw scores)
* `GET /api/exams/gradebook` (Fetches student/parent grade report card)

---

### Module 2: Homework & Submissions System

#### [NEW] [Homework.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/Homework.java)
Entity mapping teacher assignments with descriptions, class sections, subjects, and due dates.

#### [NEW] [HomeworkSubmission.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/HomeworkSubmission.java)
Entity mapping student uploads, feedback, marks, and grading parameters.

#### [NEW] Repositories:
* `HomeworkRepository.java`
* `HomeworkSubmissionRepository.java`

#### [NEW] DTOs under `com.schoolerp.school_erp.dto`:
* `HomeworkCreateRequest.java`
* `HomeworkSubmissionRequest.java`
* `HomeworkResponse.java`

#### [NEW] [HomeworkService.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/HomeworkService.java) & [HomeworkServiceImpl.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/impl/HomeworkServiceImpl.java)
Manages homework publishing, submission deadlines, and student/teacher interactions.

#### [NEW] [HomeworkController.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/controller/HomeworkController.java)
* `POST /api/homework` (Teacher publishes task)
* `POST /api/homework/{id}/submit` (Student uploads submission details/links)
* `PUT /api/homework/submission/{id}/grade` (Teacher grades submission)

---

### Module 3: Payment Gateways & Orders

#### [NEW] [PaymentGateway.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/PaymentGateway.java)
Stores configuration credentials per provider type (Stripe, Razorpay) mapped per tenant.

#### [NEW] [PaymentOrder.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/PaymentOrder.java)
Handles transaction reference IDs and checkout request payloads.

#### [NEW] Repositories:
* `PaymentGatewayRepository.java`
* `PaymentOrderRepository.java`

#### [NEW] [PaymentService.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/PaymentService.java) & [PaymentServiceImpl.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/impl/PaymentServiceImpl.java)
Generates mock Stripe/Razorpay sessions, updates billing ledger invoices, and tracks payment statuses.

#### [NEW] [PaymentController.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/controller/PaymentController.java)
* `POST /api/payments/checkout` (Generates gateway order session)
* `POST /api/payments/verify` (Handles callback notification updates)

---

### Module 4: Board Announcements & Alerts

#### [NEW] [Announcement.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/entity/Announcement.java)
Entity for public notices or section-targeted announcements.

#### [NEW] [AnnouncementRepository.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/repository/AnnouncementRepository.java)

#### [NEW] [AnnouncementService.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/AnnouncementService.java) & [AnnouncementServiceImpl.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/service/impl/AnnouncementServiceImpl.java)

#### [NEW] [AnnouncementController.java](file:///Users/shubhamverma/Workspace%20Active%20Projects/school-erp/src/main/java/com/schoolerp/school_erp/controller/AnnouncementController.java)
* `POST /api/announcements` (Post notice)
* `GET /api/announcements` (Lists context-specific announcements for user dashboard)

---

## Verification Plan

### Automated Build Checks
Validate code compilation and lack of circular dependencies:
```bash
./mvnw clean compile
```

### Mock API Verification
Run tests against database schema updates and verify controller mappings.
