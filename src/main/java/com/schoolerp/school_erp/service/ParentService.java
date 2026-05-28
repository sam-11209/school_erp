package com.schoolerp.school_erp.service;

import com.schoolerp.school_erp.dto.ParentChildResponse;
import java.util.List;
import java.util.UUID;

public interface ParentService {
    List<ParentChildResponse> getChildren(UUID parentUserId);
}
