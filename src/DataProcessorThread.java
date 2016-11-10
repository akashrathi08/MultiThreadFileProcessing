import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DataProcessorThread implements Runnable {

	public boolean SIGTERM = false;
	public volatile Map<String, List<String>> fileStatus = new HashMap<String, List<String>>() {
		{
		put("waitingFile", new ArrayList<String>());
		put("processingFile", new ArrayList<String>());
		put("processedFile", new ArrayList<String>());
		}
	};
	public volatile Map<String, List<String>> threadStatus = new HashMap<String, List<String>>() {
		{
			put("executingThread", new ArrayList<String>());
			put("completedThread", new ArrayList<String>());
			put("processedThread", new ArrayList<String>());
		}
	};
	//Holder to put the input data.
	public volatile Map<String, Map<String, Integer>> dataMap = new HashMap<>();

	public DataProcessorThread(List<String> fileList) {
		fileStatus.put("waitingFile", fileList);
		restoreState();
	}

	private void restoreState() {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream("dataMap.ser");
			ObjectInputStream ois = new ObjectInputStream(fin);
			dataMap = (Map<String, Map<String, Integer>>) ois.readObject();

			fin = new FileInputStream("fileStatus.ser");
			ois = new ObjectInputStream(fin);
			fileStatus = (Map<String, List<String>>) ois.readObject();
		} catch (FileNotFoundException e) {
			System.out.println("No state to restore");
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error reading state.");
		}finally {
			try {
				if (fin != null)
					fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	@SuppressWarnings("static-access")
	@Override
	public void run() {
		String threadName = Thread.currentThread().getName();
		System.out.println("Thread name: " + Thread.currentThread().getName());
		// Updating thread to executing once started.
		updateThreadStatus(threadName, "executingThread");

		// Reads the file in waiting list and starts processing it
		while (fileStatus.get("waitingFile").size() > 0 && !SIGTERM) {
			readFiles(threadName, threadStatus, fileStatus);
			System.out.println("------------file completed-------------");
		}
		if (SIGTERM){
			updateThreadStatus(Thread.currentThread().getName(), "completedThread");
			return;
		}
		//Updating threads to completed on finishing reading all files
		if (fileStatus.get("waitingFile").size() == 0)
			updateThreadStatus(threadName, "completedThread");

		while (threadStatus.get("completedThread").size() != 3 && threadStatus.get("processedThread").size() == 0) {
			try {
				System.out.println(threadName + " waiting");
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//Computing all required stats and updating thread status to Processed.
		if (threadStatus.get("processedThread").size() == 0){
			computeAndPrint();
		}

	}
	//Final computation after reading all the files
	public synchronized void computeAndPrint() {
		System.out.println("--------Processed Output-----------");
		for (Map.Entry entry : dataMap.entrySet()) {
			String domain = (String) entry.getKey();
			Map<String, Integer> hourMap = (Map<String, Integer>) entry.getValue();
			int maxCount = (int) Collections.max(hourMap.values(), null);
			double avg = sum(hourMap) / hourMap.keySet().size();
			System.out.format("%32s%10d%16f%10d", domain, sum(hourMap), avg, maxCount);
			System.out.println("");
		}
		updateThreadStatus(Thread.currentThread().getName(), "processedThread");
	}

	public int sum(Map<String, Integer> hourMap) {
		int sum = 0;
		for (int f : hourMap.values()) {
			sum += f;
		}
		return sum;
	}
	//Method for updating thread status
	public synchronized void updateThreadStatus(String threadName, String status) {
		if (status.equals("executingThread"))
			threadStatus.get(status).add(threadName);
		else if (status.equals("completedThread")) {
			threadStatus.get("executingThread").remove(threadName);
			threadStatus.get(status).add(threadName);
		} else if (status.equals("processedThread")) {
			threadStatus.get("completedThread").remove(threadName);
			threadStatus.get(status).add(threadName);
		}
	}
	//Reading files and mapping it to required structure 
	public synchronized void readFiles(String threadName, Map<String, List<String>> threadStatus,
			Map<String, List<String>> fileStatus) {

		List<String> waitingFiles = fileStatus.get("waitingFile");
		if (waitingFiles.size() > 0) {
			try {
				String processingFile = waitingFiles.get(0);
				Scanner sc = new Scanner(new File(processingFile));
				fileStatus.get("processingFile").add(processingFile);
				waitingFiles.remove(0);
				while (sc.hasNextLine()) {
					Thread.currentThread().sleep(100);
					String line = sc.nextLine();
					String[] wordArray = line.split("\t");
					addToDataMap(wordArray);
				}
				fileStatus.get("processedFile").add(processingFile);
				fileStatus.get("processingFile").remove(processingFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	//Putting input data into dataMap
	public void addToDataMap(String[] wordArray) {
		String date = epochToDate(wordArray[0]);
		if (dataMap.containsKey(wordArray[1])) {
			if (dataMap.get(wordArray[1]).containsKey(date)) {
				int count = dataMap.get(wordArray[1]).get(date);
				count++;
				dataMap.get(wordArray[1]).put(date, count);
			} else {
				dataMap.get(wordArray[1]).put(date, 1);
			}
		} else {
			Map<String, Integer> hourMap = new HashMap<>();
			hourMap.put(date, 1);
			dataMap.put(wordArray[1], hourMap);
		}
	}

	public String epochToDate(String epochTime) {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh");
		Date rowDate = new Date(Long.parseLong(epochTime.replaceAll("\\.[0-9]*$", "")) * 1000L);
		String dateString = format.format(rowDate);
		return dateString;
	}

}