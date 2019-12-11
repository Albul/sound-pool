package com.olekdia.soundpool;

import org.junit.rules.ExternalResource;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingResource;

public class ExecutorTestRule extends ExternalResource {

    public ExecutorTestRule(ThreadPoolExecutor executor) {
        super();
        workerExecutor = executor;
    }

    private ThreadPoolExecutor workerExecutor;
    IdlingResource idlingResource;

    @Override
    protected void before() throws Throwable {
        final ResourceCallbackExecutor workerResourceCallbackExecutor =
            new ResourceCallbackExecutor(workerExecutor);

        idlingResource = new IdlingResource() {

            @Override
            public String getName() {
                return "ExecutorIdlingResource";
            }

            @Override
            public boolean isIdleNow() {
                return workerExecutor.getActiveCount() == 0
                    && workerExecutor.getQueue().isEmpty();
            }

            @Override
            public void registerIdleTransitionCallback(IdlingResource.ResourceCallback callback) {
                workerResourceCallbackExecutor.setCallback(callback);
            }
        };
        Espresso.registerIdlingResources(idlingResource);

    }

    @Override
    protected void after() {
        Espresso.unregisterIdlingResources(idlingResource);
    }

    private static class ResourceCallbackExecutor implements Executor {
        private final Executor executor;
        private IdlingResource.ResourceCallback callback;

        ResourceCallbackExecutor(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(@NonNull final Runnable command) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    command.run();
                    callback.onTransitionToIdle();
                }
            });
        }

        void setCallback(IdlingResource.ResourceCallback callback) {
            this.callback = callback;
        }
    }
}