package com.zyq.chirp.common.util;

import com.github.rholder.retry.*;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientException;
import java.sql.SQLSyntaxErrorException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class RetryUtil {
    @Getter
    private static final Class[] NEVER_SUCCESS_SQL_ERRORS = new Class[]{
            SQLNonTransientException.class,
            SQLSyntaxErrorException.class,
            SQLIntegrityConstraintViolationException.class
    };

    public static void doDBRetry(Callable callable) throws ExecutionException, RetryException {
        doDBRetry(3, 1, TimeUnit.SECONDS, callable);
    }

    public static void doDBRetry(int maxRetryTimes, long sleepTime, TimeUnit unit, Callable callable) throws ExecutionException, RetryException {
        Retryer<Object> retryer = RetryerBuilder.newBuilder()
                .retryIfRuntimeException()
                .retryIfException(e -> {
                    Throwable cause = e.getCause();
                    for (Class ex : NEVER_SUCCESS_SQL_ERRORS) {
                        if (ex.isInstance(cause)) {
                            return false;
                        }
                    }
                    return true;
                })
                .withWaitStrategy(WaitStrategies.fixedWait(sleepTime, unit))
                .withStopStrategy(StopStrategies.stopAfterAttempt(maxRetryTimes))
                .build();
        retryer.call(callable);

    }

}
