package com.example.quartz.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SecondJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Long startTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Thread.sleep(2000);
            Long endTime = System.currentTimeMillis();
            System.out.println("执行任务SecondJob，执行时间：" + format.format(new Date(startTime)) + ",耗时:" + (endTime-startTime));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}