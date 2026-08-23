package com.githubAccess.githubAccessReport.service;

import com.githubAccess.githubAccessReport.client.GitHubClient;
import com.githubAccess.githubAccessReport.dto.AccessReportResponse;
import com.githubAccess.githubAccessReport.dto.GitHubCollaboratorResponse;
import com.githubAccess.githubAccessReport.dto.GitHubRepositoryResponse;
import com.githubAccess.githubAccessReport.dto.RepositoryAccess;
import com.githubAccess.githubAccessReport.dto.UserAccessReport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class AccessReportService {

    private final GitHubClient gitHubClient;

    /*
     * Bounded thread pool.
     *
     * At most 10 GitHub collaborator requests can run
     * concurrently.
     */
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(10);

    public AccessReportService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    public AccessReportResponse generateAccessReport(
            String organization) {

        List<GitHubRepositoryResponse> repositories =
                gitHubClient.getRepositories(organization);

        Map<String, List<RepositoryAccess>> userAccessMap =
                new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();

        /*
         * Process repositories concurrently.
         */
        for (GitHubRepositoryResponse repository : repositories) {

            Future<?> future = executorService.submit(() -> {

                try {

                    List<GitHubCollaboratorResponse> collaborators =
                            gitHubClient.getCollaborators(
                                    organization,
                                    repository.name()
                            );

                    if (collaborators == null) {
                        return;
                    }

                    for (GitHubCollaboratorResponse collaborator
                            : collaborators) {

                        String username = collaborator.login();

                        String permission =
                                determinePermission(
                                        collaborator.permissions()
                                );

                        RepositoryAccess repositoryAccess =
                                new RepositoryAccess(
                                        repository.name(),
                                        permission
                                );

                        userAccessMap
                                .computeIfAbsent(
                                        username,
                                        key -> Collections.synchronizedList(
                                                new ArrayList<>()
                                        )
                                )
                                .add(repositoryAccess);
                    }

                } catch (Exception e) {

                    /*
                     * Don't silently ignore repository failures.
                     * Log them while allowing other repositories
                     * to continue processing.
                     */
                    System.err.println(
                            "Failed to process repository "
                                    + repository.name()
                                    + ": "
                                    + e.getMessage()
                    );
                }
            });

            futures.add(future);
        }

        waitForTasks(futures);

        /*
         * Convert aggregated map into API response.
         */
        List<UserAccessReport> users =
                userAccessMap.entrySet()
                        .stream()
                        .map(entry ->
                                new UserAccessReport(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .toList();

        return new AccessReportResponse(
                organization,
                users
        );
    }

    private String determinePermission(
            GitHubCollaboratorResponse.Permissions permissions) {

        if (permissions.admin()) {
            return "ADMIN";
        }

        if (permissions.push()) {
            return "WRITE";
        }

        if (permissions.pull()) {
            return "READ";
        }

        return "NONE";
    }

    private void waitForTasks(
            List<Future<?>> futures) {

        for (Future<?> future : futures) {

            try {

                future.get();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new RuntimeException(
                        "Access report processing was interrupted",
                        e
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Failed to process access report",
                        e
                );
            }
        }
    }

    /*
     * Used by unit tests.
     */
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    /*
     * Used by the controller for the repository endpoint.
     */
    public List<GitHubRepositoryResponse> getRepositories(
            String organization) {

        return gitHubClient.getRepositories(organization);
    }

    /*
     * Used by the controller for the collaborator endpoint.
     */
    public List<GitHubCollaboratorResponse> getCollaborators(
            String owner,
            String repository) {

        return gitHubClient.getCollaborators(
                owner,
                repository
        );
    }
}