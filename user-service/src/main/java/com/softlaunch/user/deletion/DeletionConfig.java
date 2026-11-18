package com.softlaunch.user.deletion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
class DeletionConfig {

    static final String EMAIL_EXECUTOR = "deletionEmailExecutor";

    /**
     * Background workers for deletion emails. Small and bounded on purpose: if someone floods the page,
     * extra requests are refused (logged + counted) instead of piling up in memory.
     */
    @Bean(name = EMAIL_EXECUTOR, destroyMethod = "shutdown")
    ExecutorService deletionEmailExecutor() {
        return new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200),
                Thread.ofPlatform().name("deletion-email-", 0).daemon().factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
