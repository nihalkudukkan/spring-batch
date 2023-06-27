package com.infybuzz.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.infybuzz.listner.FirstJobListner;
import com.infybuzz.listner.FirstStepListner;
import com.infybuzz.processor.FirstItemProcessor;
import com.infybuzz.reader.FirstItemReader;
import com.infybuzz.service.SecondTasklet;
import com.infybuzz.writer.FirstItemWriter;

@Configuration
public class SampleJob {
	
	@Autowired
	private SecondTasklet secondTasklet;
	
	@Autowired
	private FirstJobListner firstJobListner;
	
	@Autowired
	private FirstStepListner firstStepListner;
	
	@Autowired
	private FirstItemReader firstItemReader;
	
	@Autowired
	private FirstItemProcessor firstItemProcessor;
	
	@Autowired
	private FirstItemWriter firstItemWriter;
	
	@Bean
	public Job firstJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
		return new JobBuilder("First Job")
				.repository(jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(firstStep(jobRepository,platformTransactionManager))
				.next(secondStep(jobRepository,platformTransactionManager))
				.listener(firstJobListner)
				.build();
	}
	
	
	public Step firstStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
		return new StepBuilder("First step")
				.repository(jobRepository)
				.tasklet(firstTasklet)
				.transactionManager(platformTransactionManager)
				.listener(firstStepListner)
				.build();
	}
	
	public Tasklet firstTasklet = (a,b) -> {
		System.out.println("First tasklet");
		System.out.println("SEC : " + b.getStepContext().getStepExecutionContext());
		return RepeatStatus.FINISHED;
	};

	
	public Step secondStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
		return new StepBuilder("Second step")
				.repository(jobRepository)
				.tasklet(secondTasklet)
				.transactionManager(platformTransactionManager)
				.build();
	}
	/*
	 * 
	 * 
	 */
	@Bean
	public Job secondJob(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
		return new JobBuilder("Second Job")
				.repository(jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(firstChunkStep(jobRepository, platformTransactionManager))
//				.next(secondStep(jobRepository, platformTransactionManager))
				.build();
	}
	
	private Step firstChunkStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
		return new StepBuilder("First Chunk Step")
				.repository(jobRepository)
				.transactionManager(platformTransactionManager)
				.<Integer, Long>chunk(3)
				.reader(firstItemReader)
				.processor(firstItemProcessor)
				.writer(firstItemWriter)
				.build();
	}
	
}
