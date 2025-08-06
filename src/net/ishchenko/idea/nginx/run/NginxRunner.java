package net.ishchenko.idea.nginx.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.AsyncProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import static org.jetbrains.concurrency.Promises.resolvedPromise;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 17.10.2009
 * Time: 19:44:31
 */
public class NginxRunner extends AsyncProgramRunner<RunnerSettings> {

    @Override
    @NotNull
    public String getRunnerId() {
        return "NginxRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NginxRunConfiguration;
    }

    @Override
    protected @NotNull Promise<RunContentDescriptor> execute(@NotNull ExecutionEnvironment environment,
            @NotNull RunProfileState runProfileState) throws ExecutionException {
        Executor executor = environment.getExecutor();
        return resolvedPromise(this.showRunContent(runProfileState.execute(executor, this), environment));

    }


    public RunContentDescriptor showRunContent(ExecutionResult executionResult, ExecutionEnvironment environment) {
        RunContentBuilder runContentBuilder = new RunContentBuilder(executionResult, environment);
        return runContentBuilder.showRunContent(environment.getContentToReuse());
    }

}
