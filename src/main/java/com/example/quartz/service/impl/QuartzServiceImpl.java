package com.example.quartz.service.impl;

import com.example.quartz.dto.JobInfo;
import com.example.quartz.service.QuartzService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class QuartzServiceImpl implements QuartzService {

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void startScheduler() {
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 增加一个job
     */
    @Override
    public void addJob(JobInfo jobInfo) throws SchedulerException, ClassNotFoundException {
        Objects.requireNonNull(jobInfo, "任务信息不能为空");
        // 生成job key
        JobKey jobKey = JobKey.jobKey(jobInfo.getJobName(), jobInfo.getJobGroup());
        // 当前任务不存在才进行添加
        if (!scheduler.checkExists(jobKey)) {
            Class<Job> jobClass = (Class<Job>)Class.forName(jobInfo.getClassName());
            //构建任务明细
            JobDetail jobDetail = JobBuilder
                    .newJob(jobClass)
                    .withIdentity(jobKey)
                    .withDescription(jobInfo.getJobName())
                    .build();
            // 设置job参数
            if(StringUtils.hasText(jobInfo.getConfig())){
                jobDetail.getJobDataMap().put("config",jobInfo.getConfig());
                log.info("jobDataMap: {}", jobDetail.getJobDataMap().getWrappedMap());
            }
            // 定义触发器
            TriggerKey triggerKey = TriggerKey.triggerKey(jobInfo.getTriggerName(), jobInfo.getTriggerGroup());
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobInfo.getCron()))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);

        }else{
            throw new SchedulerException(jobInfo.getJobName() + "任务已存在，无需重复添加");
        }
    }

    /**
     * 修改 一个job的 时间表达式
     *
     * @param jobName
     * @param jobGroupName
     * @param jobTime
     */
    @Override
    public void updateJob(String jobName, String jobGroupName, String jobTime) {
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroupName);
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            log.info("new jobTime: {}", jobTime);
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobTime)).build();
            // 重启触发器
            scheduler.rescheduleJob(triggerKey, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("update job error!");
        }
    }


    /**
     * 删除任务
     */
    @Override
    public boolean deleteJob(String jobGroup, String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            return scheduler.deleteJob(jobKey);
        }
        return false;
    }



    /**
     * 任务暂停
     */
    public String pauseJob(String jobGroup, String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
            return "ok";
        }else{
            return "任务不存在";
        }
    }


    /**
     * 继续任务
     */
    @Override
    public void continueJob(String jobGroup, String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
        }
    }


    /**
     * 立即执行一个job
     *
     * @param jobName
     * @param jobGroupName
     */
    @Override
    public void runAJobNow(String jobName, String jobGroupName) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("run a job error!");
        }
    }

    /**
     * 获取所有计划中的任务列表
     *
     * @return
     */
    @Override
    public List<Map<String, Object>> queryAllJob() {
        List<Map<String, Object>> jobList = null;
        try {
            GroupMatcher<JobKey> matcher = GroupMatcher.anyJobGroup();
            Set<JobKey> jobKeys = scheduler.getJobKeys(matcher);
            jobList = new ArrayList<Map<String, Object>>();
            for (JobKey jobKey : jobKeys) {
                log.info("maps: {}", scheduler.getJobDetail(jobKey).getJobDataMap().getWrappedMap());
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                for (Trigger trigger : triggers) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("jobName", jobKey.getName());
                    map.put("jobGroupName", jobKey.getGroup());
                    map.put("description", "触发器:" + trigger.getKey());
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    map.put("jobStatus", triggerState.name());
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        String cronExpression = cronTrigger.getCronExpression();
                        map.put("jobTime", cronExpression);
                    }
                    jobList.add(map);
                }
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("query all jobs error!");
        }
        return jobList;
    }

    /**
     * 获取某个任务信息
     */
    @Override
    public JobInfo getJobInfo(String jobGroup, String jobName) throws SchedulerException, JsonProcessingException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        if (!scheduler.checkExists(jobKey)) {
            return null;
        }
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        if (Objects.isNull(triggers)) {
            throw new SchedulerException("未获取到触发器信息");
        }
        TriggerKey triggerKey = triggers.get(0).getKey();
        Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(jobGroup);
        jobInfo.setJobGroup(jobName);
        jobInfo.setTriggerName(triggerKey.getName());
        jobInfo.setTriggerGroup(triggerKey.getGroup());
        jobInfo.setClassName(jobDetail.getJobClass().getName());
        jobInfo.setStatus(triggerState.toString());

        if (Objects.nonNull(jobDetail.getJobDataMap())) {
            jobInfo.setConfig(new ObjectMapper().writeValueAsString(jobDetail.getJobDataMap()));
        }

        CronTrigger theTrigger = (CronTrigger) triggers.get(0);
        jobInfo.setCron(theTrigger.getCronExpression());
        return jobInfo;
    }


    /**
     * 获取所有正在运行的job
     *
     * @return
     */
    @Override
    public List<Map<String, Object>> queryRunJob() {
        List<Map<String, Object>> jobList = null;
        try {
            List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
            jobList = new ArrayList<Map<String, Object>>(executingJobs.size());
            for (JobExecutionContext executingJob : executingJobs) {
                Map<String, Object> map = new HashMap<String, Object>();
                JobDetail jobDetail = executingJob.getJobDetail();
                JobKey jobKey = jobDetail.getKey();
                Trigger trigger = executingJob.getTrigger();
                map.put("jobName", jobKey.getName());
                map.put("jobGroupName", jobKey.getGroup());
                map.put("description", "触发器:" + trigger.getKey());
                Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                map.put("jobStatus", triggerState.name());
                if (trigger instanceof CronTrigger) {
                    CronTrigger cronTrigger = (CronTrigger) trigger;
                    String cronExpression = cronTrigger.getCronExpression();
                    map.put("jobTime", cronExpression);
                }
                jobList.add(map);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("query run jobs error!");
        }
        return jobList;
    }

}