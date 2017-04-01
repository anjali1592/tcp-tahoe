/**
 * author:Anjali Pachpute
 * Fcntcp class handles the command line arguments.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class fcntcp {
	public static void main(String args[]) throws NumberFormatException,
			IOException, ClassNotFoundException, NoSuchAlgorithmException {
		fcntcp fp = new fcntcp();
		String file = "null";
		boolean quiet = false;
		int timeout = 0;
		// if argument is -c UDPClient class is started.
		if (args[0].equals("-c")) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-f")) {
					file = args[i + 1];

				}
				if (args[i].equals("-timeout")) {
					timeout = Integer.parseInt(args[i + 1]);
				} else {
					timeout = 1000;
				}
				if (args[i].equals("-quiet")) {
					quiet = true;
				}

			}
			Thread t1 = new Thread(new UDPClient(args[args.length - 2],
					Integer.parseInt(args[args.length - 1]), file, timeout,
					quiet));
			Thread t2 = new Thread(new UDPClient(args[args.length - 2],
					Integer.parseInt(args[args.length - 1]), file, timeout,
					quiet));
			t1.start();
			t2.start();
			// System.out.println("quiet in fcntcp: "+quiet);

		}
		// If argument is -s then UDPServer class is started.
		if (args[0].equals("-s")) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-quiet")) {
					quiet = true;
				}

			}
			Thread t1 = new Thread(new UDPServer(
					Integer.parseInt(args[args.length - 1]), quiet));
			Thread t2 = new Thread(new UDPServer(
					Integer.parseInt(args[args.length - 1]), quiet));
			t1.start();
			t2.start();
		}
	}

}
