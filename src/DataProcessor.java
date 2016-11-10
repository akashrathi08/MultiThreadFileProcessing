import java.util.ArrayList;
import java.util.List;
/*
 * Main class for processing of files.
 */
public class DataProcessor {
	public static void main(String[] args) {
		//List of input files
		List<String> fileList = new ArrayList<>();
		fileList.add("src/distil-exercise-01.tsv");
		fileList.add("src/distil-exercise-02.tsv");
		fileList.add("src/distil-exercise-03.tsv");
		fileList.add("src/distil-exercise-04.tsv");
		fileList.add("src/distil-exercise-05.tsv");
		
		//Setting all the input files to waiting status
		DataProcessorThread dp = new DataProcessorThread(fileList);
		Thread t1 = new Thread(dp, "thread1");
		Thread t2 = new Thread(dp, "thread2");
		Thread t3 = new Thread(dp, "thread3");
		
		t1.start();
		t2.start();
		t3.start();
		
		Thread userInputThread = new Thread(new UserInput(dp), "userInputThread");
		userInputThread.start();
		
	}

}
