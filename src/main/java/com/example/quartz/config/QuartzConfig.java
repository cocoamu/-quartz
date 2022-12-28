package com.example.quartz.config;

import com.example.quartz.job.FirstJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class QuartzConfig {
    private static final String ID = "SUMMERDAY";

    /**
     * ① 创建Job对象
     */
    @Bean
    public JobDetail jobDetail1(){
        return JobBuilder.newJob(FirstJob.class)
                .withIdentity(ID + " 01")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger1() {
        // 简单的调度计划的构造器
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(5) // 频率
                .repeatForever(); // 次数

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail1())
                .withIdentity(ID + " 01Trigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}