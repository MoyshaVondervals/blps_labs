package ru.itmo.ordermanagement.config;

import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.JobBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.ordermanagement.job.CheckCourierTimeoutJob;
import ru.itmo.ordermanagement.job.CheckSellerTimeoutJob;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail checkSellerTimeoutJobDetail() {
        return JobBuilder.newJob(CheckSellerTimeoutJob.class)
                .withIdentity("checkSellerTimeoutJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger checkSellerTimeoutTrigger(JobDetail checkSellerTimeoutJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(checkSellerTimeoutJobDetail)
                .withIdentity("checkSellerTimeoutTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(60_000)
                        .repeatForever())
                .build();
    }

    @Bean
    public JobDetail checkCourierTimeoutJobDetail() {
        return JobBuilder.newJob(CheckCourierTimeoutJob.class)
                .withIdentity("checkCourierTimeoutJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger checkCourierTimeoutTrigger(JobDetail checkCourierTimeoutJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(checkCourierTimeoutJobDetail)
                .withIdentity("checkCourierTimeoutTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(60_000)
                        .repeatForever())
                .build();
    }
}
