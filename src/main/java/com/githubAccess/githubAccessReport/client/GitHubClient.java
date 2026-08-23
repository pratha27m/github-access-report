package com.githubAccess.githubAccessReport.client;

import com.githubAccess.githubAccessReport.dto.GitHubCollaboratorResponse;
import com.githubAccess.githubAccessReport.dto.GitHubRepositoryResponse;
import com.githubAccess.githubAccessReport.exception.GitHubApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubClient {

    private final WebClient webClient;

    public GitHubClient(WebClient githubWebClient) {
        this.webClient = githubWebClient;
    }

    public List<GitHubRepositoryResponse> getRepositories(
            String organization) {

        List<GitHubRepositoryResponse> allRepositories =
                new ArrayList<>();

        int page = 1;
        int perPage = 100;

        while (true) {

            final int currentPage = page;

            List<GitHubRepositoryResponse> repositories =
                    webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/orgs/{org}/repos")
                                    .queryParam("page", currentPage)
                                    .queryParam("per_page", perPage)
                                    .build(organization))
                            .retrieve()

                            // 401 Unauthorized
                            .onStatus(
                                    status -> status.value() == 401,
                                    response -> response
                                            .bodyToMono(String.class)
                                            .map(body ->
                                                    new GitHubApiException(
                                                            HttpStatus.UNAUTHORIZED,
                                                            "GitHub authentication failed"
                                                    ))
                            )

                            // 403 Forbidden
                            .onStatus(
                                    status -> status.value() == 403,
                                    response -> response
                                            .bodyToMono(String.class)
                                            .map(body ->
                                                    new GitHubApiException(
                                                            HttpStatus.FORBIDDEN,
                                                            "GitHub access forbidden for organization: "
                                                                    + organization
                                                    ))
                            )

                            // 404 Not Found
                            .onStatus(
                                    status -> status.value() == 404,
                                    response -> response
                                            .bodyToMono(String.class)
                                            .map(body ->
                                                    new GitHubApiException(
                                                            HttpStatus.NOT_FOUND,
                                                            "GitHub organization not found: "
                                                                    + organization
                                                    ))
                            )

                            .bodyToFlux(
                                    GitHubRepositoryResponse.class)
                            .collectList()
                            .block();

            if (repositories == null || repositories.isEmpty()) {
                break;
            }

            allRepositories.addAll(repositories);

            if (repositories.size() < perPage) {
                break;
            }

            page++;
        }

        return allRepositories;
    }

    public List<GitHubCollaboratorResponse> getCollaborators(
            String owner,
            String repository) {

        return webClient.get()
                .uri(
                        "/repos/{owner}/{repo}/collaborators",
                        owner,
                        repository
                )
                .retrieve()

                // 401 Unauthorized
                .onStatus(
                        status -> status.value() == 401,
                        response -> response
                                .bodyToMono(String.class)
                                .map(body ->
                                        new GitHubApiException(
                                                HttpStatus.UNAUTHORIZED,
                                                "GitHub authentication failed"
                                        ))
                )

                // 403 Forbidden
                .onStatus(
                        status -> status.value() == 403,
                        response -> response
                                .bodyToMono(String.class)
                                .map(body ->
                                        new GitHubApiException(
                                                HttpStatus.FORBIDDEN,
                                                "GitHub access forbidden for repository: "
                                                        + owner
                                                        + "/"
                                                        + repository
                                        ))
                )

                // 404 Not Found
                .onStatus(
                        status -> status.value() == 404,
                        response -> response
                                .bodyToMono(String.class)
                                .map(body ->
                                        new GitHubApiException(
                                                HttpStatus.NOT_FOUND,
                                                "GitHub repository not found: "
                                                        + owner
                                                        + "/"
                                                        + repository
                                        ))
                )

                .bodyToFlux(GitHubCollaboratorResponse.class)
                .collectList()
                .block();
    }
}