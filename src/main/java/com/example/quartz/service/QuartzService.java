package com.example.quartz.service;

import com.example.quartz.dto.JobInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.Map;

public interface QuartzService {

    /**
     * 增加一个任务job
     */
    void addJob(JobInfo jobInfo)throws SchedulerException, ClassNotFoundException;

    /**
     * 修改一个任务job
     * @param jobName 任务名称
     * @param jobGroupName  任务组名
     * @param jobTime   cron时间表达式
     */
    void updateJob(String jobName, String jobGroupName, String jobTime);

    /**
     * 删除一个任务job
     * @param jobGroup
     * @param jobName
     */
    boolean deleteJob(String jobGroup, String jobName) throws SchedulerException;

    /**
     * 暂停一个任务job
     * @param jobName
     * @param jobGroupName
     */
    String pauseJob(String jobName, String jobGroupName)throws SchedulerException;

    /**
     * 恢复一个任务job
     * @param jobGroup
     * @param jobName
     */
    void continueJob(String jobGroup, String jobName) throws SchedulerException;

    /**
     * 立即执行一个任务job
     * @param jobName
     * @param jobGroupName
     */
    void runAJobNow(String jobName, String jobGroupName);

    /**
     * 获取所有任务job
     * @return
     */
    List<Map<String, Object>> queryAllJob();

    /**
     * 获取正在运行的任务job
     * @return
     */
    List<Map<String, Object>> queryRunJob();

    JobInfo getJobInfo(String jobGroup, String jobName) throws SchedulerException, JsonProcessingException;
}