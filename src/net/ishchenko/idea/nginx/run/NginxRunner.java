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
import com.intellij.execution.ui.ExecutionUiService;
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
        // RunProfileState state = environment.getState();
        return resolvedPromise(this.showRunContent(runProfileState.execute(executor, this), environment));

    }
    // @Override
    // public void execute(@NotNull ExecutionEnvironment environment, Callback callback, RunProfileState state) {
    //     // 获取执行器
    //     Executor executor = environment.getExecutor();
    //     // 获取运行配置状态
    //     // RunProfileState state = environment.getState();
    //     if(state == null) {
    //         return;
    //     }
    //
    //     ExecutionManager.getInstance(environment.getProject())
    //             .startRunProfile(environment, () -> {
    //                 try {
    //                     return resolvedPromise(this.showRunContent(state.execute(executor, this), environment));
    //                 } catch(ExecutionException e) {
    //                     throw new RuntimeException(e);
    //                 }
    //             });
    // }

    public RunContentDescriptor showRunContent(ExecutionResult executionResult, ExecutionEnvironment environment) {
        return ExecutionUiService.getInstance()
                .showRunContent(executionResult, environment);
    }

}
