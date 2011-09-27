package module.metaWorkflow.activities;

import module.metaWorkflow.domain.WorkflowMetaProcess;
import module.workflow.activities.ActivityInformation;
import module.workflow.activities.WorkflowActivity;
import myorg.domain.User;

public class EditFieldValue extends WorkflowActivity<WorkflowMetaProcess, ActivityInformation<WorkflowMetaProcess>> {

    @Override
    public String getUsedBundle() {
	return "resources/MetaWorkflowResources";
    }

    @Override
    public boolean isActive(WorkflowMetaProcess process, User user) {
	return process.isAccessibleToCurrentUser();
    }

    @Override
    public ActivityInformation<WorkflowMetaProcess> getActivityInformation(WorkflowMetaProcess process) {
	return new EditFieldValueInfo(process, this);
    }

    @Override
    public boolean isUserAwarenessNeeded(WorkflowMetaProcess process) {
	return false;
    }

    @Override
    public boolean isDefaultInputInterfaceUsed() {
	return false;
    }

    @Override
    public boolean isVisible() {
	return false;
    }

    @Override
    protected void process(ActivityInformation<WorkflowMetaProcess> activityInformation) {
    }
}
