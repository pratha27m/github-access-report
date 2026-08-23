package com.githubAccess.githubAccessReport.client;

import com.githubAccess.githubAccessReport.dto.GitHubCollaboratorResponse;
import com.githubAccess.githubAccessReport.dto.GitHubRepositoryResponse;
import com.githubAccess.githubAccessReport.exception.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;

class GitHubClientTest {


    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private ExchangeFunction exchangeFunction;

    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() {

        exchangeFunction = mock(ExchangeFunction.class);

        WebClient webClient =
                WebClient.builder()
                        .baseUrl("https://api.github.com")
                        .exchangeFunction(exchangeFunction)
                        .build();

        gitHubClient = new GitHubClient(webClient);
    }

    // ---------------------------------------------------------
    // getRepositories() tests
    // ---------------------------------------------------------

    @Test
    void shouldHandleRepositoryPagination()  throws Exception{

        // GitHub returns 100 repositories on the first page
        List<GitHubRepositoryResponse> firstPage =
                new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            firstPage.add(
                    new GitHubRepositoryResponse(
                            "repo-" + i,
                            "cloud-eagle-java-test/repo-" + i
                    )
            );
        }

        // GitHub returns 1 repository on the second page
        List<GitHubRepositoryResponse> secondPage =
                List.of(
                        new GitHubRepositoryResponse(
                                "repo-101",
                                "cloud-eagle-java-test/repo-101"
                        )
                );

        String firstPageJson =
                objectMapper.writeValueAsString(firstPage);

        String secondPageJson =
                objectMapper.writeValueAsString(secondPage);

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(
                        Mono.just(
                                createResponse(
                                        HttpStatus.OK,
                                        firstPageJson
                                )
                        )
                )
                .thenReturn(
                        Mono.just(
                                createResponse(
                                        HttpStatus.OK,
                                        secondPageJson
                                )
                        )
                );

        List<GitHubRepositoryResponse> repositories =
                gitHubClient.getRepositories(
                        "cloud-eagle-java-test"
                );

        assertNotNull(repositories);

        assertEquals(101, repositories.size());

        assertEquals(
                "repo-1",
                repositories.get(0).name()
        );

        assertEquals(
                "repo-101",
                repositories.get(100).name()
        );

        verify(exchangeFunction, times(2))
                .exchange(any(ClientRequest.class));
    }

    @Test
    void shouldSendPaginationParameters() {

        String responseBody = """
                [
                    {
                        "name": "repo-1",
                        "full_name": "cloud-eagle-java-test/repo-1"
                    }
                ]
                """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.OK,
                                responseBody
                        )
                ));

        gitHubClient.getRepositories(
                "cloud-eagle-java-test"
        );

        ArgumentCaptor<ClientRequest> captor =
                ArgumentCaptor.forClass(ClientRequest.class);

        verify(exchangeFunction)
                .exchange(captor.capture());

        String uri =
                captor.getValue()
                        .url()
                        .toString();

        assertTrue(uri.contains("page=1"));
        assertTrue(uri.contains("per_page=100"));
        assertTrue(uri.contains(
                "/orgs/cloud-eagle-java-test/repos"
        ));
    }

    // ---------------------------------------------------------
    // getCollaborators() tests
    // ---------------------------------------------------------

    @Test
    void shouldGetCollaboratorsSuccessfully() {

        String responseBody = """
                [
                    {
                        "login": "pratha27m",
                        "permissions": {
                            "pull": true,
                            "push": true,
                            "admin": true
                        }
                    },
                    {
                        "login": "lashkare27",
                        "permissions": {
                            "pull": true,
                            "push": false,
                            "admin": false
                        }
                    }
                ]
                """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.OK,
                                responseBody
                        )
                ));

        List<GitHubCollaboratorResponse> collaborators =
                gitHubClient.getCollaborators(
                        "cloud-eagle-java-test",
                        "access-report-test"
                );

        assertNotNull(collaborators);

        assertEquals(2, collaborators.size());

        assertEquals(
                "pratha27m",
                collaborators.get(0).login()
        );

        assertEquals(
                "lashkare27",
                collaborators.get(1).login()
        );

        assertTrue(
                collaborators.get(0)
                        .permissions()
                        .admin()
        );

        assertTrue(
                collaborators.get(1)
                        .permissions()
                        .pull()
        );

        assertFalse(
                collaborators.get(1)
                        .permissions()
                        .push()
        );
    }

    // ---------------------------------------------------------
    // Error handling tests
    // ---------------------------------------------------------

    @Test
    void shouldThrowExceptionWhenAuthenticationFails() {

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.UNAUTHORIZED,
                                """
                                {
                                    "message": "Bad credentials"
                                }
                                """
                        )
                ));

        GitHubApiException exception =
                assertThrows(
                        GitHubApiException.class,
                        () -> gitHubClient.getRepositories(
                                "cloud-eagle-java-test"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("authentication failed")
        );
    }

    @Test
    void shouldThrowExceptionWhenOrganizationAccessIsForbidden() {

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.FORBIDDEN,
                                """
                                {
                                    "message": "Forbidden"
                                }
                                """
                        )
                ));

        GitHubApiException exception =
                assertThrows(
                        GitHubApiException.class,
                        () -> gitHubClient.getRepositories(
                                "cloud-eagle-java-test"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("forbidden")
        );

        assertTrue(
                exception.getMessage()
                        .contains("cloud-eagle-java-test")
        );
    }

    @Test
    void shouldThrowExceptionWhenOrganizationDoesNotExist() {

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.NOT_FOUND,
                                """
                                {
                                    "message": "Not Found"
                                }
                                """
                        )
                ));

        GitHubApiException exception =
                assertThrows(
                        GitHubApiException.class,
                        () -> gitHubClient.getRepositories(
                                "unknown-organization"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("organization not found")
        );
    }

    @Test
    void shouldThrowExceptionWhenRepositoryAccessIsForbidden() {

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.FORBIDDEN,
                                """
                                {
                                    "message": "Forbidden"
                                }
                                """
                        )
                ));

        GitHubApiException exception =
                assertThrows(
                        GitHubApiException.class,
                        () -> gitHubClient.getCollaborators(
                                "cloud-eagle-java-test",
                                "access-report-test"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("forbidden")
        );

        assertTrue(
                exception.getMessage()
                        .contains("cloud-eagle-java-test")
        );

        assertTrue(
                exception.getMessage()
                        .contains("access-report-test")
        );
    }

    @Test
    void shouldThrowExceptionWhenRepositoryDoesNotExist() {

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.NOT_FOUND,
                                """
                                {
                                    "message": "Not Found"
                                }
                                """
                        )
                ));

        GitHubApiException exception =
                assertThrows(
                        GitHubApiException.class,
                        () -> gitHubClient.getCollaborators(
                                "cloud-eagle-java-test",
                                "unknown-repository"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("repository not found")
        );
    }

    @Test
    void shouldThrowExceptionWhenCollaboratorAuthenticationFails() {

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(
                        createResponse(
                                HttpStatus.UNAUTHORIZED,
                                """
                                {
                                    "message": "Bad credentials"
                                }
                                """
                        )
                ));

        GitHubApiException exception =
                assertThrows(
                        GitHubApiException.class,
                        () -> gitHubClient.getCollaborators(
                                "cloud-eagle-java-test",
                                "access-report-test"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("authentication failed")
        );
    }

    // ---------------------------------------------------------
    // Helper method
    // ---------------------------------------------------------

    private ClientResponse createResponse(
            HttpStatus status,
            String body) {

        return ClientResponse
                .create(status)
                .header(
                        "Content-Type",
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(body)
                .build();
    }
}