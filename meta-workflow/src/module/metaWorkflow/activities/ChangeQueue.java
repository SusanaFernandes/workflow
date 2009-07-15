package module.metaWorkflow.activities;

import module.metaWorkflow.domain.WorkflowMetaProcess;
import module.metaWorkflow.domain.WorkflowQueue;
import module.workflow.activities.WorkflowActivity;
import myorg.domain.User;

public class ChangeQueue extends WorkflowActivity<WorkflowMetaProcess, ChangeQueueInformation> {

    @Override
    public boolean isActive(WorkflowMetaProcess process, User user) {
	return process.isAccessible(user) && !process.isUserObserver(user);
    }

    @Override
    protected void process(ChangeQueueInformation activityInformation) {
	activityInformation.getProcess().setCurrentQueue(activityInformation.getQueue());
    }

    @Override
    public String getUsedBundle() {
	return "resources/MetaWorkflowResources";
    }

    @Override
    public ChangeQueueInformation getActivityInformation(WorkflowMetaProcess process) {
	return new ChangeQueueInformation(process, this);
    }

    @Override
    protected String[] getArgumentsDescription(ChangeQueueInformation activityInformation) {
	WorkflowQueue currentQueue = activityInformation.getProcess().getCurrentQueue();
	return new String[] { currentQueue != null ? currentQueue.getName() : "", activityInformation.getQueue().getName() };
    }
}
