package com.githubAccess.githubAccessReport.dto;

public record GitHubCollaboratorResponse(
        String login,
        Permissions permissions
) {

    public record Permissions(
            boolean pull,
            boolean push,
            boolean admin
    ) {
    }
}
