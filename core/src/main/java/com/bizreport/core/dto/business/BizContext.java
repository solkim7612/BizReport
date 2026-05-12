package com.bizreport.core.dto.business;

import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.history.BizHistory;

public record BizContext(
        Users user,
        BizHistory before,
        BizHistory after
) {}