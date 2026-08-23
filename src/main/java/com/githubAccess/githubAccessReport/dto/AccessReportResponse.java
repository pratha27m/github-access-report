package com.githubAccess.githubAccessReport.dto;

import java.util.List;

public record AccessReportResponse(
        String organization,
        List<UserAccessReport> users
) {
}