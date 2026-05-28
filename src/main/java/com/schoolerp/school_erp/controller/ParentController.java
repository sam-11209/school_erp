package com.schoolerp.school_erp.controller;

import com.schoolerp.school_erp.dto.ParentChildResponse;
import com.schoolerp.school_erp.security.RequiresRoles;
import com.schoolerp.school_erp.security.UserContext;
import com.schoolerp.school_erp.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller to handle features and actions accessible by parent users.
 * 
 * Constraints:
 * - Requires a valid authentication token under Authorization header.
 * - Requires X-Tenant-ID header to resolve database tenant context.
 * - Restricted to users with the PARENT role.
 */
@RestController
@RequestMapping("/api/parent")
@RequiresRoles("PARENT")
public class ParentController {

    @Autowired
    private ParentService parentService;

    /**
     * Retrieves all student children mapped to the currently authenticated parent.
     * 
     * Constraints:
     * - Requires standard JWT in Authorization header.
     * - Requires X-Tenant-ID header.
     * - Accessible only by users with the PARENT role.
     * 
     * @return list of children details including their personal, academic class, and section information
     */
    @GetMapping("/children")
    public ResponseEntity<List<ParentChildResponse>> getChildren() {
        UUID parentUserId = UserContext.getCurrentUser();
        List<ParentChildResponse> children = parentService.getChildren(parentUserId);
        return ResponseEntity.ok(children);
    }
}
