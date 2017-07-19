package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.IOException;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public abstract class MergeRequestNotifier extends Notifier implements MatrixAggregatable {
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        GitLabApi client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No GitLab connection configured");
            return true;
        }
        Integer projectId = getProjectId(build);
        Integer mergeRequestId = getMergeRequestId(build);
        if (projectId != null && mergeRequestId != null) {
            perform(build, listener, client, projectId, mergeRequestId);
        }
        return true;
    }

    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                perform(build, launcher, listener);
                return super.endBuild();
            }
        };
    }

    protected abstract void perform(Run<?, ?> build, TaskListener listener, GitLabApi client, Integer projectId, Integer mergeRequestId);

    Integer getProjectId(Run<?, ?> build) {
        GitLabWebHookCause cause = getCauseRecursive(build.getCauses());
        return cause == null ? null : cause.getData().getTargetProjectId();
    }

    Integer getMergeRequestId(Run<?, ?> build) {
        GitLabWebHookCause cause = getCauseRecursive(build.getCauses());
        return cause == null ? null : cause.getData().getMergeRequestId();
    }
    
    GitLabWebHookCause getCauseRecursive(List<Cause> causes) {
        for (Cause cause : causes) {
            if (cause instanceof GitLabWebHookCause) {
                return cause;
            }
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                return getCauseRecursive(upstreamCause.getUpstreamCauses());
            }
        }
        return null;
    }
}
