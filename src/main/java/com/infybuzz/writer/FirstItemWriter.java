package com.infybuzz.writer;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.infybuzz.model.StudentJdbc;
import com.infybuzz.model.StudentResponse;

@Component
public class FirstItemWriter implements ItemWriter<StudentJdbc> {

	@Override
	public void write(List<? extends StudentJdbc> items) throws Exception {
		System.out.println("Inside item writer");
		items.stream().forEach(System.out::println);
	}

}
