package org.hit.internetprogramming.eoh.server.action;

import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Thread service to be used by graph algorithms that we run in the server.<br/>
 * This class is a singleton created for serving parallel algorithms. This is not the thread pool
 * of out TCP server. Instead, this is a thread pool that serves parallel actions.<br/>
 * We need to share a thread pool among all actions, to make sure we do not maintain a thread pool
 * for every action (user request) with no management over the amount of threads that the server created.
 * Thus we can set a limit here which is shared for all actions.
 */
public class ActionThreadService implements ExecutorService {
    private static final int THREADS_PER_PROCESSOR = 10;
    private final ExecutorService threadPool;

    @Getter
    private final int amountOfWorkers;

    private ActionThreadService() {
        amountOfWorkers = Runtime.getRuntime().availableProcessors() * THREADS_PER_PROCESSOR;

        // Create a new cached thread pool, but use bounded max pool size, so we will not create too many threads.
        threadPool = new ThreadPoolExecutor(0, amountOfWorkers, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    /**
     * @return The unique instance of {@link ActionThreadService}.
     */
    public static ActionThreadService getInstance() {
        return ActionThreadServiceHolder.INSTANCE;
    }

    @Override
    public void shutdown() {
        threadPool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return threadPool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return threadPool.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return threadPool.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return threadPool.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return threadPool.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return threadPool.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return threadPool.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return threadPool.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return threadPool.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return threadPool.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return threadPool.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        threadPool.execute(command);
    }

    private static class ActionThreadServiceHolder {
        static final ActionThreadService INSTANCE = new ActionThreadService();
    }
}