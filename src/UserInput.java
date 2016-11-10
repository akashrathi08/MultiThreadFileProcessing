import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map;

/*
 * Thread class for taking SIGTERM user input
 */
public class UserInput implements Runnable {
	
	DataProcessorThread dph;
	
	public UserInput(DataProcessorThread dph) {
		this.dph = dph;
	}
	
	@Override
	public void run() {
		
		DataInputStream dis = new DataInputStream(System.in);
		
		try {
			
			while (true) {
				System.out.println("Type SIGTERM to stop execution...");
				String str = dis.readLine();
				
				if (str.equalsIgnoreCase("SIGTERM")) {
					dph.SIGTERM = true;
					
					while(dph.threadStatus.get("completedThread").size() != 3);
					
					FileOutputStream fout = new FileOutputStream("dataMap.ser");
					ObjectOutputStream oos = new ObjectOutputStream(fout);
					oos.writeObject(dph.dataMap);
					oos.close();
					
					FileOutputStream fout2 = new FileOutputStream("fileStatus.ser");
					ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
					oos2.writeObject(dph.fileStatus);
					oos2.close();
					
					break;
				} else {
					System.out.println("Invalid Command");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
