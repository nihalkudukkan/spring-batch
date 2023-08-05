package com.infybuzz.config;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.infybuzz.listner.FirstJobListner;
import com.infybuzz.listner.FirstStepListner;
import com.infybuzz.model.StudentCsv;
import com.infybuzz.model.StudentJdbc;
import com.infybuzz.model.StudentJson;
import com.infybuzz.model.StudentResponse;
import com.infybuzz.processor.FirstItemProcessor;
import com.infybuzz.reader.FirstItemReader;
import com.infybuzz.service.StudentService;
import com.infybuzz.writer.FirstItemWriter;

@Configuration
public class SampleJob {
	
	@Autowired
	private FirstItemReader firstItemReader;
	
	@Autowired
	private FirstItemProcessor firstItemProcessor;
	
	@Autowired
	private FirstItemWriter firstItemWriter;
	
	@Autowired
	private StudentService studentService;
	
	@Bean
	@Primary
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().build();
	}
	
	@Bean
	@ConfigurationProperties(prefix = "spring.universitydatasource")
	public DataSource universitydatasource() {
		return DataSourceBuilder.create().build();
	}
	
	@Bean
	public Job ChunkJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new JobBuilder("Chunk Job")
				.repository(jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(firstChunkStep(jobRepository, transactionManager))
				.build();
	}
	
	private Step firstChunkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("First Chunk Step")
				.repository(jobRepository)
				.transactionManager(transactionManager)
				.<StudentCsv, StudentCsv>chunk(1)
				.reader(flatFileItemReader(null))
//				.reader(jsonItemReader(null))
//				.reader(jdbcCursorItemReader())
//				.reader(itemReaderAdapter())
//				.processor(firstItemProcessor)
//				.writer(firstItemWriter)
//				.writer(flatFileItemWriter(null))
//				.writer(jsonFileItemWriter(null))
				.writer(jdbcBatchItemWriter())
//				.writer(jdbcBatchItemWriter1())
				.build();		
	}
	
	@StepScope
	@Bean
	public FlatFileItemReader<StudentCsv> flatFileItemReader(
				@Value("${inputFile}") FileSystemResource inputFile
			) {
		FlatFileItemReader<StudentCsv> flatFileItemReader = new FlatFileItemReader<StudentCsv>();
		flatFileItemReader.setResource(inputFile);
		flatFileItemReader.setLineMapper(new DefaultLineMapper<StudentCsv>() {
			{
				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames("ID", "First Name", "Last Name", "Email");
					}
				});
				
				setFieldSetMapper(new BeanWrapperFieldSetMapper<StudentCsv>() {
					{
						setTargetType(StudentCsv.class);
					}
				});
			}
		});
		flatFileItemReader.setLinesToSkip(1);
		return flatFileItemReader;
	}
	
	@StepScope
	@Bean
	public JsonItemReader<StudentCsv> jsonItemReader(
			@Value("${inputFile}") FileSystemResource inputFile
			) {
		JsonItemReader<StudentCsv> itemReader = new JsonItemReader<StudentCsv>();
		itemReader.setResource(inputFile);
		itemReader.setJsonObjectReader(new JacksonJsonObjectReader<StudentCsv>(StudentCsv.class));
		
		return itemReader;
	}
	
	public JdbcCursorItemReader<StudentJdbc> jdbcCursorItemReader() {
		JdbcCursorItemReader<StudentJdbc> jdbcCursorItemReader = new JdbcCursorItemReader<StudentJdbc>();
		jdbcCursorItemReader.setDataSource(universitydatasource());
		jdbcCursorItemReader.setSql("select id, firstName, lastName, email from students");
		jdbcCursorItemReader.setRowMapper(new BeanPropertyRowMapper<StudentJdbc>() {
			{
				setMappedClass(StudentJdbc.class);
			}
		});
		return jdbcCursorItemReader;
	}
	
	/*
	public ItemReaderAdapter<StudentResponse> itemReaderAdapter() {
		ItemReaderAdapter<StudentResponse> itemReaderAdapter = new ItemReaderAdapter<StudentResponse>();
		
		itemReaderAdapter.setTargetObject(studentService);
		itemReaderAdapter.setTargetMethod("getStudent");
		
		return itemReaderAdapter;
	}
	*/
	
	@StepScope
	@Bean
	public FlatFileItemWriter<StudentJdbc> flatFileItemWriter(@Value("${outputFile}") FileSystemResource fileSystemResource) {
		FlatFileItemWriter<StudentJdbc> flatFileItemWriter = new FlatFileItemWriter<StudentJdbc>();
		flatFileItemWriter.setResource(fileSystemResource);
		flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
			public void writeHeader(Writer writer) throws IOException {
				writer.write("ID,First Name,Last Name,Email");
			}
		});
		flatFileItemWriter.setLineAggregator(new DelimitedLineAggregator<StudentJdbc>() {
			{
				setFieldExtractor(new BeanWrapperFieldExtractor<StudentJdbc>() {
					{
						setNames(new String[] {"id", "firstName", "lastName", "email"});
					}
				});
			}
		});
		flatFileItemWriter.setFooterCallback(new FlatFileFooterCallback() {
			public void writeFooter(Writer writer) throws IOException {
				writer.write("Created @ " + new Date());
			}
		});
		return flatFileItemWriter;
	}
	
	@StepScope
	@Bean
	public JsonFileItemWriter<StudentJson> jsonFileItemWriter(@Value("${outputFile}") FileSystemResource fileSystemResource) {
		JsonFileItemWriter<StudentJson> jsonFileItemWriter = new JsonFileItemWriter<StudentJson>(fileSystemResource,
				new JacksonJsonObjectMarshaller<StudentJson>());
		return jsonFileItemWriter;
	}
	
//	@StepScope
	@Bean
	public JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter() {
		JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter = new JdbcBatchItemWriter<StudentCsv>();
		jdbcBatchItemWriter.setDataSource(universitydatasource());
		jdbcBatchItemWriter.setSql("insert into students(id,firstName,lastName,email) values (:id,:firstName,:lastName,:email)");
//		jdbcBatchItemWriter.setSql("update students set firstName=:firstName,lastName=:lastName,email=:email where id=:id");
		jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<StudentCsv>());
		return jdbcBatchItemWriter;
	}
	
	@Bean
	public JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter1() {
		JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter = new JdbcBatchItemWriter<StudentCsv>();
		jdbcBatchItemWriter.setDataSource(universitydatasource());
		jdbcBatchItemWriter.setSql("insert into students(id,firstName,lastName,email) values (?,?,?,?)");
//		jdbcBatchItemWriter.setSql("update students set firstName=:firstName,lastName=:lastName,email=:email where id=:id");
//		jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<StudentCsv>());
		jdbcBatchItemWriter.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<StudentCsv>() {
			
			@Override
			public void setValues(StudentCsv item, PreparedStatement ps) throws SQLException {
				ps.setLong(1, item.getId());
				ps.setString(2, item.getFirstName());
				ps.setString(2, item.getLastName());
				ps.setString(4, item.getEmail());
			}
		});
		return jdbcBatchItemWriter;
	}
}
