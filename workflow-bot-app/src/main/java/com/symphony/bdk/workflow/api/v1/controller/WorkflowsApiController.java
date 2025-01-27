package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.WorkflowsApi;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;
import com.symphony.bdk.workflow.security.Authorized;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/workflows")
@Slf4j
@RequiredArgsConstructor
public class WorkflowsApiController implements WorkflowsApi {

  private final MonitoringService monitoringService;
  private final WorkflowEngine<CamundaTranslatedWorkflowContext> workflowEngine;

  @Override
  public ResponseEntity<Object> executeWorkflowById(String token, String id, WorkflowExecutionRequest arguments) {
    log.info("Executing workflow {}", id);
    workflowEngine.execute(id, new ExecutionParameters(arguments.getArgs(), token));

    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<List<WorkflowView>> listAllWorkflows(String token) {
    return ResponseEntity.ok(monitoringService.listAllWorkflows());
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<List<WorkflowInstView>> listWorkflowInstances(String workflowId, String token, String status,
      Long version) {
    return ResponseEntity.ok(monitoringService.listWorkflowInstances(workflowId, status, version));
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<WorkflowNodesView> getInstanceState(String workflowId, String instanceId,
      String token, Instant startedBefore, Instant startedAfter, Instant finishedBefore, Instant finishedAfter) {
    WorkflowInstLifeCycleFilter lifeCycleFilter =
        new WorkflowInstLifeCycleFilter(startedBefore, startedAfter, finishedBefore, finishedAfter);

    return ResponseEntity.ok(monitoringService.listWorkflowInstanceNodes(workflowId, instanceId, lifeCycleFilter));
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<WorkflowDefinitionView> getWorkflowDefinition(String workflowId, String token, Long version) {
    return ResponseEntity.ok(monitoringService.getWorkflowDefinition(workflowId, version));
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<List<VariableView>> listWorkflowGlobalVariables(String workflowId, String instanceId,
      String token, Instant updatedBefore, Instant updatedAfter) {
    return ResponseEntity.ok(
        monitoringService.listWorkflowInstanceGlobalVars(workflowId, instanceId, updatedBefore, updatedAfter));
  }

}
