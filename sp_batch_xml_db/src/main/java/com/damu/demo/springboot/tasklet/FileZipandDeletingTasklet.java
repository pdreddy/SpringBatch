package com.damu.demo.springboot.tasklet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class FileZipandDeletingTasklet implements Tasklet {
	private String resourcesPath;
	public String getResourcesPath() {
		return resourcesPath;
	}

	public void setResourcesPath(String resourcesPath) {
		this.resourcesPath = resourcesPath;
	}
	private String zipPath ;

	public String getZipPath() {
		return zipPath;
	}

	public void setZipPath(String zipPath) {
		this.zipPath = zipPath;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		long count = 0;
		try (Stream<Path> files = Files.list(Paths.get(resourcesPath))) {
			count = files.count();
		}
		if (count > 0) {
		zipDirectory(resourcesPath, zipPath);
		Files.walk(Paths.get(resourcesPath)).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);
		}{
		}
		return RepeatStatus.FINISHED;

	}

	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	public static void zipDirectory(String sourceDirectoryPath, String zipPath) throws IOException {
		String folderName="";
		System.err.println(sourceDirectoryPath+"---"+sourceDirectoryPath.contains("error"));
		if(sourceDirectoryPath.contains("error")) {
			folderName="factory_feed_ingestion_error_";
		}
		else if(sourceDirectoryPath.contains("success")) {
			folderName="factory_feed_ingestion_success_";
		}
		Path zipFilePath = Files.createFile(Paths.get(zipPath +folderName +getCurrentTimeStamp() + ".zip"));
			
			try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
				Path sourceDirPath = Paths.get(sourceDirectoryPath);

				Files.walk(sourceDirPath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
					ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
					try {
						zipOutputStream.putNextEntry(zipEntry);
						zipOutputStream.write(Files.readAllBytes(path));
						zipOutputStream.closeEntry();

					} catch (Exception e) {
					}
				});
			}
		
	}
}
