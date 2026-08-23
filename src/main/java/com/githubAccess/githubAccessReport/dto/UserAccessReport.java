package com.githubAccess.githubAccessReport.dto;

import java.util.List;

public record UserAccessReport(
        String username,
        List<RepositoryAccess> repositories
) {
}