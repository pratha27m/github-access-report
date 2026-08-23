package com.githubAccess.githubAccessReport.service;

import com.githubAccess.githubAccessReport.client.GitHubClient;
import com.githubAccess.githubAccessReport.dto.AccessReportResponse;
import com.githubAccess.githubAccessReport.dto.GitHubCollaboratorResponse;
import com.githubAccess.githubAccessReport.dto.GitHubRepositoryResponse;
import com.githubAccess.githubAccessReport.dto.RepositoryAccess;
import com.githubAccess.githubAccessReport.dto.UserAccessReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AccessReportServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    private AccessReportService accessReportService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        accessReportService =
                new AccessReportService(gitHubClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        accessReportService.shutdownExecutor();
    }

    @Test
    void shouldGenerateAccessReport() {

        GitHubRepositoryResponse repository =
                new GitHubRepositoryResponse(
                        "access-report-test",
                        "cloud-eagle-java-test/access-report-test"
                );

        GitHubCollaboratorResponse admin =
                new GitHubCollaboratorResponse(
                        "pratha27m",
                        new GitHubCollaboratorResponse.Permissions(
                                true,
                                true,
                                true
                        )
                );

        GitHubCollaboratorResponse write =
                new GitHubCollaboratorResponse(
                        "prathmeshlashkare27",
                        new GitHubCollaboratorResponse.Permissions(
                                true,
                                true,
                                false
                        )
                );

        GitHubCollaboratorResponse read =
                new GitHubCollaboratorResponse(
                        "lashkare27",
                        new GitHubCollaboratorResponse.Permissions(
                                true,
                                false,
                                false
                        )
                );

        when(gitHubClient.getRepositories(
                "cloud-eagle-java-test"))
                .thenReturn(List.of(repository));

        when(gitHubClient.getCollaborators(
                "cloud-eagle-java-test",
                "access-report-test"))
                .thenReturn(List.of(admin, write, read));

        AccessReportResponse response =
                accessReportService.generateAccessReport(
                        "cloud-eagle-java-test");

        assertEquals(
                "cloud-eagle-java-test",
                response.organization()
        );

        assertEquals(3, response.users().size());
    }

    @Test
    void shouldMapAdminPermissionCorrectly() {

        GitHubRepositoryResponse repository =
                new GitHubRepositoryResponse(
                        "access-report-test",
                        "cloud-eagle-java-test/access-report-test"
                );

        GitHubCollaboratorResponse collaborator =
                new GitHubCollaboratorResponse(
                        "pratha27m",
                        new GitHubCollaboratorResponse.Permissions(
                                true,
                                true,
                                true
                        )
                );

        when(gitHubClient.getRepositories(
                "cloud-eagle-java-test"))
                .thenReturn(List.of(repository));

        when(gitHubClient.getCollaborators(
                "cloud-eagle-java-test",
                "access-report-test"))
                .thenReturn(List.of(collaborator));

        AccessReportResponse response =
                accessReportService.generateAccessReport(
                        "cloud-eagle-java-test");

        UserAccessReport user =
                response.users().get(0);

        RepositoryAccess access =
                user.repositories().get(0);

        assertEquals("pratha27m", user.username());
        assertEquals("ADMIN", access.permission());
    }

    @Test
    void shouldMapWritePermissionCorrectly() {

        GitHubRepositoryResponse repository =
                new GitHubRepositoryResponse(
                        "access-report-test",
                        "cloud-eagle-java-test/access-report-test"
                );

        GitHubCollaboratorResponse collaborator =
                new GitHubCollaboratorResponse(
                        "prathmeshlashkare27",
                        new GitHubCollaboratorResponse.Permissions(
                                true,
                                true,
                                false
                        )
                );

        when(gitHubClient.getRepositories(
                "cloud-eagle-java-test"))
                .thenReturn(List.of(repository));

        when(gitHubClient.getCollaborators(
                "cloud-eagle-java-test",
                "access-report-test"))
                .thenReturn(List.of(collaborator));

        AccessReportResponse response =
                accessReportService.generateAccessReport(
                        "cloud-eagle-java-test");

        UserAccessReport user =
                response.users().get(0);

        assertEquals(
                "WRITE",
                user.repositories().get(0).permission()
        );
    }

    @Test
    void shouldMapReadPermissionCorrectly() {

        GitHubRepositoryResponse repository =
                new GitHubRepositoryResponse(
                        "access-report-test",
                        "cloud-eagle-java-test/access-report-test"
                );

        GitHubCollaboratorResponse collaborator =
                new GitHubCollaboratorResponse(
                        "lashkare27",
                        new GitHubCollaboratorResponse.Permissions(
                                true,
                                false,
                                false
                        )
                );

        when(gitHubClient.getRepositories(
                "cloud-eagle-java-test"))
                .thenReturn(List.of(repository));

        when(gitHubClient.getCollaborators(
                "cloud-eagle-java-test",
                "access-report-test"))
                .thenReturn(List.of(collaborator));

        AccessReportResponse response =
                accessReportService.generateAccessReport(
                        "cloud-eagle-java-test");

        UserAccessReport user =
                response.users().get(0);

        assertEquals(
                "READ",
                user.repositories().get(0).permission()
        );
    }

    @Test
    void shouldReturnEmptyUsersWhenOrganizationHasNoRepositories() {

        when(gitHubClient.getRepositories(
                "cloud-eagle-java-test"))
                .thenReturn(List.of());

        AccessReportResponse response =
                accessReportService.generateAccessReport(
                        "cloud-eagle-java-test");

        assertEquals(
                "cloud-eagle-java-test",
                response.organization()
        );

        assertTrue(response.users().isEmpty());
    }
}