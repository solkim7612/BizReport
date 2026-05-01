package com.bizreport.core.dto.business;

import com.bizreport.core.entity.user.User;
import com.bizreport.core.entity.history.BizHistory;

public record BizContext(
        User user,
        BizHistory before,
        BizHistory after
) {}