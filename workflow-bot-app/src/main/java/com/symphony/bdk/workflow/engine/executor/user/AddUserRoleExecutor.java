package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.core.service.user.constant.RoleId;
import com.symphony.bdk.workflow.engine.executor.AbstractActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.AddUserRole;

import lombok.extern.slf4j.Slf4j;

/**
 * Leads to multiple API calls, so the execution could be incomplete or under performant if a lot of users are passed.
 */
@Slf4j
public class AddUserRoleExecutor extends AbstractActivityExecutor<AddUserRole>
    implements ActivityExecutor<AddUserRole> {

  @Override
  public void execute(ActivityExecutorContext<AddUserRole> context) {
    AddUserRole userRole = context.getActivity();

    for (Long userId : userRole.getUserIds()) {
      for (String role : userRole.getRoles()) {
        log.debug("Adding role {} to user {}", role, userId);
        context.bdk().users().addRole(userId, RoleId.valueOf(role));
      }
    }
  }

}
