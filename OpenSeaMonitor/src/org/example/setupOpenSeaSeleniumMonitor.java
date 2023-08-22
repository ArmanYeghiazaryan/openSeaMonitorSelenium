package org.example;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class setupOpenSeaSeleniumMonitor {

	public static void main(String[] args) throws Exception {

//		Console console = System.console();
//		if (console == null && !GraphicsEnvironment.isHeadless()) {
//			String filename = Main.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
//			Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", "cmd", "/k",
//					"java -jar \"" + "\\src\\org\\example\\setupAldi.java" + "\"" });
//		}

		System.out.println("Starting OPENSEA_MONITOR engine...");
		Path path = Paths.get("tasks.csv");
		Reader in = new FileReader("tasks.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		int taskId = 0;

		System.out.println("Initializing " + Integer.valueOf((int) Files.lines(path).count() - 1) + " task(s)...\n");
		Thread.sleep(1000);

		for (CSVRecord record : records) {

			taskId++;
			Thread a = new OpenseaSeleniumMonitorTask(record.get("collectionLink"), taskId,
					record.get("strivingProfit"), record.get("webhookUrl"));
			a.setName(String.valueOf(taskId));
			a.start();

		}

	}

}
