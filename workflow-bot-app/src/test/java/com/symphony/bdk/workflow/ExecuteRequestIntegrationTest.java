package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiResponse;
import com.symphony.bdk.http.api.util.TypeReference;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

class ExecuteRequestIntegrationTest extends IntegrationTest {

  private static final String OUTPUTS_STATUS_KEY = "%s.outputs.status";
  private static final String OUTPUTS_BODY_KEY = "%s.outputs.body";

  @Test
  void executeRequestSuccessful() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request.swadl.yaml"));

    final ApiClient mockedApiClient = mock(ApiClient.class);
    final Map<String, String> header = Map.of("headerKey", "headerValue");
    final Map<String, Object> body = Map.of("args", Map.of("key", "value"));
    final String jsonResponse =
        "{\"name\":\"john\",\"age\":22,\"contact\":{\"phone\":\"0123456\",\"email\":\"john@symphony.com\"}}";

    final ApiResponse<Object> mockedResponse = new ApiResponse<>(200, Collections.emptyMap(), jsonResponse);

    when(bdkGateway.apiClient(anyString())).thenReturn(mockedApiClient);
    when(mockedApiClient.invokeAPI(eq(""), eq("GET"), anyList(), eq(body), eq(header), anyMap(), anyMap(),
        eq("application/json"), eq("application/json"), eq(null), any(TypeReference.class)))
        .thenReturn(mockedResponse);

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_STATUS_KEY, "executeGetRequest"), 200)
        .hasOutput(String.format(OUTPUTS_BODY_KEY, "executeGetRequest"), jsonResponse);
  }

  @Test
  void executeRequestFailed() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request.swadl.yaml"));

    final ApiClient mockedApiClient = mock(ApiClient.class);

    when(bdkGateway.apiClient(anyString())).thenReturn(mockedApiClient);
    when(mockedApiClient.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ApiException(400, "Bad request error for the test", Collections.emptyMap(),
            "ApiException response body"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_STATUS_KEY, "executeGetRequest"), 400)
        .hasOutput(String.format(OUTPUTS_BODY_KEY, "executeGetRequest"), "ApiException response body");
  }
}