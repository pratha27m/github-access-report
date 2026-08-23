package com.githubAccess.githubAccessReport.controller;

import com.githubAccess.githubAccessReport.dto.AccessReportResponse;
import com.githubAccess.githubAccessReport.dto.GitHubCollaboratorResponse;
import com.githubAccess.githubAccessReport.dto.GitHubRepositoryResponse;
import com.githubAccess.githubAccessReport.service.AccessReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/github")
public class AccessReportController {

    private final AccessReportService accessReportService;


    public AccessReportController(
            AccessReportService accessReportService) {
        this.accessReportService = accessReportService;
    }

    @GetMapping("/{organization}/repos")
    public List<GitHubRepositoryResponse> getRepositories(
            @PathVariable String organization) {

        return accessReportService.getRepositories(
                organization);
    }

    @GetMapping("/{owner}/{repo}/collaborators")
    public List<GitHubCollaboratorResponse> getCollaborators(
            @PathVariable String owner,
            @PathVariable String repo) {

        return accessReportService.getCollaborators(
                owner,
                repo);
    }

    @GetMapping("/{organization}/access-report")
    public AccessReportResponse getAccessReport(
            @PathVariable String organization) {

        return accessReportService.generateAccessReport(
                organization);
    }
}
