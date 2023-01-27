package com.symphony.bdk.workflow.api.v1.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Instant;

class WorkflowsMgtApiControllerTest extends ApiTest {

  private static final String URL = "/v1/management/workflows/";

  @Test
  void test_managementRequests_deploy() throws Exception {
    doNothing().when(workflowManagementService).deploy(anyString());

    mockMvc.perform(request(HttpMethod.POST, URL)
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain")
            .content("swadl content"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).deploy(anyString());
  }

  @Test
  void test_managementRequests_update() throws Exception {
    doNothing().when(workflowManagementService).update(anyString());

    mockMvc.perform(request(HttpMethod.PUT, URL)
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain")
            .content("swadl content"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).update(anyString());
  }

  @Test
  void test_managementRequests_delete() throws Exception {
    doNothing().when(workflowManagementService).deploy(anyString());

    mockMvc.perform(request(HttpMethod.DELETE, URL + "id")
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain")
            .content("swadl content"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).delete(anyString());
  }

  @Test
  void test_missingToken_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, URL)
            .contentType("text/plain")
            .content("content"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_streamingLogs_missingToken_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.GET, URL + "/logs")
            .contentType("text/plain"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_streamingLogs_returnOk() throws Exception {
    mockMvc.perform(request(HttpMethod.GET, URL + "/logs")
            .contentType("text/plain")
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken"))
        .andExpect(status().isOk());
  }

  @Test
  void test_setActiveVersion_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, URL + "/wfid/versions/v1")
            .contentType("text/plain")
            .content("content"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_setActiveVersion_returnOk() throws Exception {
    doNothing().when(workflowManagementService).setActiveVersion(anyString(), anyString());

    mockMvc.perform(request(HttpMethod.POST, URL + "/wfId/versions/v1")
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).setActiveVersion(eq("wfId"), eq("v1"));
  }

  @Test
  void test_scheduleWorkflowExpiration_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, URL + "/wfid")
            .contentType("application/json")
            .content(Instant.now().toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_scheduleWorkflowExpiration_exceptionThrown() throws Exception {
    final String workflowId = "wfId";
    final Instant now = Instant.now();

    doThrow(NotFoundException.class).when(workflowExpirationService)
        .scheduleWorkflowExpiration(eq(workflowId), eq(now));

    mockMvc.perform(request(HttpMethod.POST, URL + "/" + workflowId)
            .contentType("application/json")
            .content(Instant.now().toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_scheduleWorkflowExpiration_returnOk() throws Exception {
    final String workflowId = "wfId";
    final Instant now = Instant.now();

    doNothing().when(workflowExpirationService).scheduleWorkflowExpiration(eq(workflowId), eq(now));
    mockMvc.perform(request(HttpMethod.POST, URL + "/" + workflowId)
        .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
        .contentType("application/json")
        .content("\"" + now.toString() + "\""))
        .andExpect(status().isOk());

    verify(workflowExpirationService).scheduleWorkflowExpiration(eq(workflowId), eq(now));
  }
}
