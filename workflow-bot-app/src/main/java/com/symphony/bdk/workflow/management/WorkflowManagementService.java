package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class WorkflowManagementService {
  private static final String WORKFLOW_NOT_EXIST_EXCEPTION_MSG = "Workflow %s does not exist.";
  private static final String WORKFLOW_UPDATE_FORBIDDEN_EXCEPTION_MSG = "Update on a published Workflow is forbidden.";
  private final WorkflowEngine<CamundaTranslatedWorkflowContext> workflowEngine;
  private final VersionedWorkflowRepository versionRepository;
  private final ObjectConverter objectConverter;

  public void deploy(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    CamundaTranslatedWorkflowContext context = workflowEngine.translate(workflow);
    throwExceptionIfUnPublishedVersionExists(workflow);
    String deploy = null;
    if (workflow.isToPublish()) {
      deploy = workflowEngine.deploy(context);
      setCurrentActiveVersionToInactive(workflow.getId());
    }
    VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, swadlView, deploy);
    versionRepository.save(versionedWorkflow);
  }

  private void throwExceptionIfUnPublishedVersionExists(Workflow workflow) {
    Optional<VersionedWorkflow> notPublished = versionRepository.findByWorkflowIdAndPublishedFalse(workflow.getId());
    notPublished.ifPresent(wf -> {
      throw new IllegalArgumentException(
          String.format("Version %s of workflow has not been published yet.", wf.getVersion()));
    });
  }

  private VersionedWorkflow toVersionedWorkflow(Workflow workflow, SwadlView swadlView, String deploymentId) {
    Optional<String> optionalDeployId = Optional.ofNullable(deploymentId);
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflow.getId());
    versionedWorkflow.setVersion(workflow.getVersion());
    versionedWorkflow.setDeploymentId(optionalDeployId.orElse(null));
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setUserId(swadlView.getAuthor());
    versionedWorkflow.setPublished(workflow.isToPublish());
    versionedWorkflow.setActive(optionalDeployId.isPresent() ? true : null);
    versionedWorkflow.setDeploymentId(optionalDeployId.orElse(null));
    return versionedWorkflow;
  }

  public void update(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    CamundaTranslatedWorkflowContext context = workflowEngine.translate(workflow);

    VersionedWorkflow versionedWorkflow = readAndValidate(workflow);
    versionedWorkflow.setVersion(workflow.getVersion());
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setPublished(workflow.isToPublish());

    if (workflow.isToPublish()) {
      String deploy = workflowEngine.deploy(context);
      setCurrentActiveVersionToInactive(workflow.getId());
      versionedWorkflow.setDeploymentId(deploy);
      versionedWorkflow.setActive(true);
    }
    versionRepository.save(versionedWorkflow);
  }

  private void setCurrentActiveVersionToInactive(String workflow) {
    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndActiveTrue(workflow);
    activeVersion.ifPresent(activeWorkflow -> {
      activeWorkflow.setActive(null);
      versionRepository.saveAndFlush(activeWorkflow);
    });
  }

  public List<VersionedWorkflowView> get(String id) {
    List<VersionedWorkflow> workflows = versionRepository.findByWorkflowId(id);
    return objectConverter.convertCollection(workflows, VersionedWorkflowView.class);
  }

  public Optional<VersionedWorkflowView> get(String id, Long version) {
    Optional<VersionedWorkflow> versionedWorkflow = versionRepository.findByWorkflowIdAndVersion(id, version);
    return versionedWorkflow.map(w -> objectConverter.convert(w, VersionedWorkflowView.class));
  }

  private VersionedWorkflow readAndValidate(Workflow workflow) {
    List<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowId(workflow.getId());
    if (activeVersion.isEmpty()) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflow.getId()));
    }

    if (activeVersion.stream().anyMatch(VersionedWorkflow::getPublished)) {
      throw new UnsupportedOperationException(WORKFLOW_UPDATE_FORBIDDEN_EXCEPTION_MSG);
    }

    // The list can only contain 1 draft version item
    return activeVersion.get(0);
  }

  public void delete(String id) {
    this.delete(id, null);
  }

  public void delete(String id, Long version) {
    Optional.ofNullable(version).ifPresentOrElse(ver -> {
      Optional<VersionedWorkflow> workflow = versionRepository.findByWorkflowIdAndVersion(id, ver);
      workflow.ifPresent(w -> {
        versionRepository.deleteByWorkflowIdAndVersion(id, ver);
        if (w.getActive()) {
          workflowEngine.undeployByDeploymentId(w.getDeploymentId());
        }
      });
    }, () -> {
      versionRepository.deleteByWorkflowId(id);
      workflowEngine.undeployByWorkflowId(id);
    });
  }

  public void setActiveVersion(String workflowId, Long version) {
    VersionedWorkflow deployedWorkflow = validateWorkflowVersion(workflowId, version);
    Workflow workflowToDeploy = objectConverter.convert(deployedWorkflow.getSwadl(), version, Workflow.class);
    String deploymentId = workflowEngine.deploy(workflowToDeploy);
    setCurrentActiveVersionToInactive(workflowId);
    deployedWorkflow.setDeploymentId(deploymentId);
    deployedWorkflow.setActive(true);
    versionRepository.save(deployedWorkflow);
  }

  private VersionedWorkflow validateWorkflowVersion(String workflowId, Long version) {
    Optional<VersionedWorkflow> optionalDeployed = versionRepository.findByWorkflowIdAndVersion(workflowId, version);
    if (optionalDeployed.isEmpty()) {
      throw new NotFoundException(String.format("Version %s of the workflow %s does not exist.", version, workflowId));
    }

    VersionedWorkflow deployedWorkflow = optionalDeployed.get();
    if (!deployedWorkflow.getPublished()) {
      throw new IllegalArgumentException(
          String.format("Version %s of the workflow %s is in draft mode.", version, workflowId));
    }
    return deployedWorkflow;
  }
}
