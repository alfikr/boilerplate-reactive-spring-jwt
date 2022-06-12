package com.jasavast.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Slf4j
public class ExceptionHandlingAsyncTaskExecutor implements AsyncTaskExecutor, InitializingBean,DisposableBean {

    private final AsyncTaskExecutor executor;
    public ExceptionHandlingAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor){
        this.executor=asyncTaskExecutor;
    }
    @Override
    public void destroy() throws Exception {
        if(executor instanceof DisposableBean){
            DisposableBean bean= (DisposableBean) executor;
            bean.destroy();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(executor instanceof InitializingBean){
            InitializingBean bean = (InitializingBean) executor;
            bean.afterPropertiesSet();
        }
    }

    @Override
    public void execute(Runnable runnable, long l) {
        executor.execute(createWrapper(runnable),l);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return executor.submit(createWrapper(runnable));
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(createCallable(callable));
    }

    @Override
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }
    private <T> Callable<T> createCallable(Callable<T> call){
        return ()->{
            try {
                return call.call();
            }catch (Exception e){
                log.error("Callable error ",e);
                throw e;
            }
        };
    }
    private Runnable createWrapper(Runnable task){
        return () -> {
            try {
                task.run();
            }catch (Exception e){
                log.error("Task error",e);
            }
        };
    }
}
