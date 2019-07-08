/**
 * 
 */
package com.damu.demo.springboot.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.damu.demo.springboot.listener.VechicleJobListener;
import com.damu.demo.springboot.listener.VechicleStepListener;
import com.damu.demo.springboot.model.Vehicle;
import com.damu.demo.springboot.model.VehicleInformation;
import com.damu.demo.springboot.processor.VehicleItemProcessor;
import com.damu.demo.springboot.tasklet.FileZipandDeletingTasklet;
import com.damu.demo.springboot.tasklet.MoveErrorFilesTasklet;
import com.damu.demo.springboot.tasklet.MoveFilesTasklet;
import com.damu.demo.springboot.util.Constants;
import com.damu.demo.springboot.util.VehiclePreparedSmtSetter;

/**
 * @author Damodhara Palavali
 *
 */
@Configuration
@EnableBatchProcessing
@EnableScheduling
public class JobConfig {
	Logger logger = LoggerFactory.getLogger(JobConfig.class);

	@Value("${files.path}")
	private String resourcesPath;
	@Value("${files.error.path}")
	private String errorPath;
	@Value("${files.success.path}")
	private String sucessPath;
	@Value("${files.zip.path}")
	private String zipPath;
	@Value("${files.type}")
	private String fileType;
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private DataSource dataSource;

	@Scheduled(cron = "${spring.batch.job.cron.expression}")
	public void ffiSchedule() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addDate("launchDate", new Date())
					.toJobParameters();
			jobLauncher.run(ffiIngestionJob(), jobParameters);
		} catch (Exception e) {
		}
	}

	@Scheduled(cron = "${spring.batch.job.cron.zip.expression}")
	public void ffiZIPSchedule() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addDate("launchDate", new Date())
					.toJobParameters();
			jobLauncher.run(ffiZipfilesJob(), jobParameters);
		} catch (Exception e) {
		}
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setCorePoolSize(5);
		taskExecutor.afterPropertiesSet();
		return taskExecutor;
	}

	public Job ffiZipfilesJob() throws Exception {
		return jobBuilderFactory.get("ffiZipfilesJob").incrementer(new RunIdIncrementer()).flow(deleteAndZipFiles()).next(deleteAndZipErrorFiles()).end()
				.build();
	}

	@Bean
	public Step deleteAndZipFiles() {
		FileZipandDeletingTasklet zipSucessFilesTasklet = new FileZipandDeletingTasklet();
			zipSucessFilesTasklet.setResourcesPath(sucessPath);
			zipSucessFilesTasklet.setZipPath(zipPath);

			return stepBuilderFactory.get("deleteAndZipFiles").tasklet(zipSucessFilesTasklet).build();
	}
	@Bean
	public Step deleteAndZipErrorFiles() {
		FileZipandDeletingTasklet zipErrorFilesTasklet = new FileZipandDeletingTasklet();
			zipErrorFilesTasklet.setResourcesPath(errorPath);
			zipErrorFilesTasklet.setZipPath(zipPath);
			return stepBuilderFactory.get("deleteAndZipErrorFiles").tasklet(zipErrorFilesTasklet).build();
	}
	@Bean
	public Job ffiIngestionJob() throws Exception {
		return jobBuilderFactory.get("ffiIngestionJob").incrementer(new RunIdIncrementer())
				.listener(new VechicleJobListener()).start(masterStep()).on("COMPLETED").to(moveFiles())
				.from(masterStep()).on("UNKNOWN").to(moveErrorFiles()).end().build();
	}

	@Bean
	public Step masterStep() throws Exception {
		return stepBuilderFactory.get("masterStep").partitioner(slaveStep()).partitioner("partition", partitioner())
				.taskExecutor(taskExecutor()).listener(new VechicleStepListener()).build();
	}

	@Bean
	public Step slaveStep() throws Exception {
		return stepBuilderFactory.get("slaveStep").<Vehicle, Vehicle>chunk(1)
				.reader(reader(null)).processor(processor(null, null)).writer(dbWriter()).build();
	}

	@Bean
	protected Step moveFiles() {
		MoveFilesTasklet moveFilesTasklet = new MoveFilesTasklet();
		try {
			moveFilesTasklet.setResourcesPath(sucessPath);
			moveFilesTasklet.setResources(new PathMatchingResourcePatternResolver().getResources("file:" + sucessPath + fileType));
		} catch (IOException e) {
			
		}
		return stepBuilderFactory.get("moveFiles").tasklet(moveFilesTasklet).build();
	}

	@Bean
	protected Step moveErrorFiles() {
		MoveErrorFilesTasklet moveFilesTasklet = new MoveErrorFilesTasklet();
		try {
			moveFilesTasklet.setResourcesPath(errorPath);
			moveFilesTasklet.setResources(new PathMatchingResourcePatternResolver().getResources("file:" + errorPath + fileType));
		} catch (IOException e) {
			
		}
		return stepBuilderFactory.get("moveErrorFiles").tasklet(moveFilesTasklet).build();
	}

	@Bean
	@JobScope
	public Partitioner partitioner() throws Exception {
		MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		partitioner.setResources(resolver.getResources("file:" + resourcesPath + fileType));
		partitioner.partition(20);
		logger.info("--------partitioner()---end -No of files--->"
				+ resolver.getResources("file:" + resourcesPath + fileType).length);
		return partitioner;
	}

	@Bean
	@StepScope
	public StaxEventItemReader<Vehicle> reader(@Value("#{stepExecutionContext['fileName']}") String file)
			throws MalformedURLException {
		logger.info("----StaxEventItemReader----fileName--->" + file);
		StaxEventItemReader<Vehicle> reader = new StaxEventItemReader<>();
		reader.setResource(new UrlResource(file));
		reader.setFragmentRootElementNames(new String[] {"Vehicle", "VehicleInformation" });
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Vehicle.class, VehicleInformation.class);
		reader.setUnmarshaller(marshaller);
		return reader;
	}
	@Bean
	@StepScope
	public VehicleItemProcessor processor(@Value("#{stepExecutionContext['fileName']}") String file,
			@Value("#{stepExecution.jobExecution.id}") String jobID) {
		String fileName = file.substring(file.lastIndexOf("/") + 1);
		VehicleItemProcessor factoryFeeditemProcessor = new VehicleItemProcessor();
		logger.debug("----processor----fileName--->" + file);
		factoryFeeditemProcessor.setProcessingFileName(fileName);
		factoryFeeditemProcessor.setProcessingJobid(jobID);
		return factoryFeeditemProcessor;
	}
	
	
	@Bean
	public JdbcBatchItemWriter<Vehicle> dbWriter() {
		JdbcBatchItemWriter<Vehicle> writer = new JdbcBatchItemWriter<>();
		writer.setDataSource(dataSource);
		writer.setSql(Constants.INSERT_QUERY_OLD);
		writer.setItemPreparedStatementSetter(new VehiclePreparedSmtSetter());
		return writer;
	}
	
}
