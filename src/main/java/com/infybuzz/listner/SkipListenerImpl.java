package com.infybuzz.listner;

import java.io.File;
import java.io.FileWriter;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import com.infybuzz.model.StudentCsv;
import com.infybuzz.model.StudentJson;

@Component
public class SkipListenerImpl implements SkipListener<StudentCsv, StudentJson> {

	@Override
	public void onSkipInRead(Throwable th) {
		if(th instanceof FlatFileParseException) {
			createFile("C:\\Users\\nihal\\Desktop\\course\\udemy\\infybuzz\\spring-batch\\Chunk Job\\First Chunk Step\\reader\\SkipInRead.txt",
					((FlatFileParseException) th).getInput());
		}
	}

	@Override
	public void onSkipInWrite(StudentJson studentJson, Throwable t) {
		createFile("C:\\Users\\nihal\\Desktop\\course\\udemy\\infybuzz\\spring-batch\\Chunk Job\\First Chunk Step\\writer\\SkipInWrite.txt",
				studentJson.toString());
	}

	@Override
	public void onSkipInProcess(StudentCsv studentCsv, Throwable t) {
		createFile("C:\\Users\\nihal\\Desktop\\course\\udemy\\infybuzz\\spring-batch\\Chunk Job\\First Chunk Step\\processor\\SkipInProcessor.txt",
				studentCsv.toString());
	}
	
	public void createFile(String filePath, String data) {
		try(FileWriter fileWriter = new FileWriter(new File(filePath), true)) {
			fileWriter.write(data + "\n");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
