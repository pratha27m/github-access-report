package com.githubAccess.githubAccessReport.controller;

import com.githubAccess.githubAccessReport.dto.AccessReportResponse;
import com.githubAccess.githubAccessReport.dto.RepositoryAccess;
import com.githubAccess.githubAccessReport.dto.UserAccessReport;
import com.githubAccess.githubAccessReport.service.AccessReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccessReportControllerTest {

    @Mock
    private AccessReportService accessReportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        AccessReportController controller =
                new AccessReportController(accessReportService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void shouldReturnAccessReport() throws Exception {

        RepositoryAccess repositoryAccess =
                new RepositoryAccess(
                        "access-report-test",
                        "ADMIN"
                );

        UserAccessReport user =
                new UserAccessReport(
                        "pratha27m",
                        List.of(repositoryAccess)
                );

        AccessReportResponse response =
                new AccessReportResponse(
                        "cloud-eagle-java-test",
                        List.of(user)
                );

        when(accessReportService.generateAccessReport(
                "cloud-eagle-java-test"
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/github/cloud-eagle-java-test/access-report")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.organization")
                        .value("cloud-eagle-java-test"))
                .andExpect(jsonPath("$.users.length()")
                        .value(1))
                .andExpect(jsonPath("$.users[0].username")
                        .value("pratha27m"))
                .andExpect(jsonPath("$.users[0].repositories.length()")
                        .value(1))
                .andExpect(jsonPath(
                        "$.users[0].repositories[0].repository"
                ).value("access-report-test"))
                .andExpect(jsonPath(
                        "$.users[0].repositories[0].permission"
                ).value("ADMIN"));
    }

    @Test
    void shouldReturnEmptyUsersWhenNoAccessExists()
            throws Exception {

        AccessReportResponse response =
                new AccessReportResponse(
                        "cloud-eagle-java-test",
                        List.of()
                );

        when(accessReportService.generateAccessReport(
                "cloud-eagle-java-test"
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/github/cloud-eagle-java-test/access-report")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization")
                        .value("cloud-eagle-java-test"))
                .andExpect(jsonPath("$.users.length()")
                        .value(0));
    }
}