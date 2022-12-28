package com.example.quartz.controller;

import com.example.quartz.dto.JobInfo;
import com.example.quartz.service.QuartzService;
import com.example.quartz.vo.ResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/job")
public class QuartzController {

    @Resource
    private QuartzService quartzService;
    @Resource
    private Scheduler scheduler;

    /**
     * http://localhost:8888/job/all
     */
    @RequestMapping("/all")
    public List<JobInfo> list() throws SchedulerException, JsonProcessingException {
        List<JobInfo> jobInfos = new ArrayList<>();
        List<String> triggerGroupNames = scheduler.getTriggerGroupNames();
        for (String triggerGroupName : triggerGroupNames) {
            Set<TriggerKey> triggerKeySet = scheduler
                    .getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroupName));
            for (TriggerKey triggerKey : triggerKeySet) {
                Trigger trigger = scheduler.getTrigger(triggerKey);
                JobKey jobKey = trigger.getJobKey();
                JobInfo jobInfo = quartzService.getJobInfo(jobKey.getGroup(), jobKey.getName());
                jobInfos.add(jobInfo);
            }
        }
        return jobInfos;
    }

    /**
     * http://localhost:8888/job/add
     *
     * {
     * 	"className": "com.example.quartz.job.MyJob",
     * 	"config": "配置信息，例如存储json",
     * 	"cron": "0/3 * * * * ?",
     * 	"jobGroup": "STANDARD_JOB_GROUP",
     * 	"jobName": "计划任务通知任务",
     * 	"triggerGroup": "STANDARD_TRIGGER_GROUP",
     * 	"triggerName": "计划任务通知触发器"
     * }
     *
     * {
     * 	"className": "com.example.quartz.job.TimeJob",
     * 	"config": "配置信息，例如存储json",
     * 	"cron": "0/10 * * * * ?",
     * 	"jobGroup": "STANDARD_JOB_GROUP",
     * 	"jobName": "时间通知任务",
     * 	"triggerGroup": "STANDARD_TRIGGER_GROUP",
     * 	"triggerName": "时间通知触发器"
     * }
     */
    @PostMapping("/add")
    public JobInfo addJob(@RequestBody JobInfo jobInfo) throws SchedulerException, ClassNotFoundException {
        quartzService.addJob(jobInfo);
        return jobInfo;
    }

    /**
     * http://localhost:8888/job/pause?jobGroup=STANDARD_JOB_GROUP&jobName=计划任务通知任务
     * http://localhost:8888/job/pause?jobGroup=STANDARD_JOB_GROUP&jobName=时间通知任务
     */
    @RequestMapping("/pause")
    public ResultVO pauseJob(@RequestParam("jobGroup") String jobGroup, @RequestParam("jobName") String jobName)
            throws SchedulerException {
       return new ResultVO(quartzService.pauseJob(jobGroup, jobName));
    }

    /**
     * http://localhost:8888/job/continue?jobGroup=STANDARD_JOB_GROUP&jobName=计划任务通知任务
     * http://localhost:8888/job/continue?jobGroup=STANDARD_JOB_GROUP&jobName=时间通知任务
     */
    @RequestMapping("/continue")
    public void continueJob(@RequestParam("jobGroup") String jobGroup, @RequestParam("jobName") String jobName)
            throws SchedulerException {
        quartzService.continueJob(jobGroup, jobName);
    }

    /**
     * http://localhost:8888/job/delete?jobGroup=STANDARD_JOB_GROUP&jobName=计划任务通知任务
     * http://localhost:8888/job/delete?jobGroup=STANDARD_JOB_GROUP&jobName=时间通知任务
     */
    @RequestMapping("/delete")
    public boolean deleteJob(@RequestParam("jobGroup") String jobGroup, @RequestParam("jobName") String jobName)
            throws SchedulerException {
        return quartzService.deleteJob(jobGroup, jobName);
    }
}