import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

/**
 * 
 * @author Anjali UDPClient sends a file to the server via its sending thread
 *         and receives acknowledgement from the Server via its receiving
 *         thread.
 */
public class UDPClient extends Thread {
	// private static final String ClientReceiver = null;
	String serveraddress;
	static int port, timeout;
	static InetAddress serAddress;
	static DatagramSocket clntsock, clntreceive;
	static DatagramPacket sendPacket;
	String filename;
	int count = 0, count1 = 0, filelength = 0;
	// int pack, offset;
	ByteArrayOutputStream ous;
	static ByteArrayOutputStream outputStream;
	static TreeMap<Integer, byte[]> hmap;
	static TreeMap<Integer, Integer> congestionWindow;
	static TreeMap<Integer, TCP> copyofhmap;
	Integer sequence;
	Object bytearray;
	MessageDigest md5;
	DatagramPacket receivep;
	private int size = 0;
	static int cwnd = 1;
	static String name = "";
	static int computedChecksum;
	static Vector a = new Vector();
	static Vector a1 = new Vector();
	byte[] byteinCongestionWindow = new byte[1460];
	TCP t = new TCP();
	static byte[] sendser = new byte[2000];
	byte[] accept = new byte[1460];
	static byte[] checksumPayload = new byte[1460];
	static Vector handlers = new Vector(10);
	static int sstresh = 50;
	static int sizeofTree;
	int sendP = 0;
	static int lastP;
	static ArrayList al = new ArrayList();
	static int cw, window;
	static int counter;
	static int previousAck, currentAck, countertill3, retransmitted;
	static boolean quiet = false, lastPSent;
	byte[] remainingData;

	/**
	 * 
	 * @param serveraddress
	 *            : Address of the server
	 * @param port
	 *            : Listening port of the server
	 * @param filename
	 *            : File sent by the client
	 * @param timeout
	 *            : timeout to receive acknowledgements from the server.1000mS
	 *            if not specified
	 * @throws SocketException
	 */
	public UDPClient(String serveraddress, int port, String filename,
			int timeout, boolean quiet) throws SocketException {
		this.serveraddress = serveraddress;
		this.port = port;
		this.filename = filename;
		this.timeout = timeout;
		this.quiet = quiet;
		clntsock = new DatagramSocket();
	}

	/**
	 * Executes 2 threads simultaneously: one for Receiving acknowledgements
	 * from the server and one for sending packets.
	 */
	public void run() {

		synchronized (handlers) {
			handlers.addElement(this);
			size = handlers.size();
		}

		if (size == 1) {
			try {
				ClientSender();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (size == 2) {
			try {
				ClientReceiver();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Divides file into bytes,attaches TCP header to the data and sends it over
	 * the network in form of packets
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NoSuchAlgorithmException
	 */
	public void ClientSender() throws IOException, InterruptedException,
			NoSuchAlgorithmException {
		int i = 0;
		try {
			serAddress = InetAddress.getByName(serveraddress);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int seq = 0, seqincopy = 0;
		Checksum(filename);
		File f = new File(filename);
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// hmap stores file divided into bytes along with its sequence number.
		hmap = new TreeMap<Integer, byte[]>();
		copyofhmap = new TreeMap<Integer, TCP>();
		// congestionWindow keeos track of the packets sent over the network.
		congestionWindow = new TreeMap<Integer, Integer>();

		try {
			while ((count = fs.read(accept)) != -1) {

				if (count != 1460) {
					remainingData = new byte[count];
					// accept=remainingData;
					System.arraycopy(accept, 0, remainingData, 0, count);
					accept = remainingData;
					hmap.put(seq++, accept);

				} else {

					hmap.put(seq++, accept);
					accept = new byte[1460];

				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Created a copy of hmap where key is a new seq number and value is the
		// TCP packet.
		for (Entry<Integer, byte[]> entry : hmap.entrySet()) {
			sizeofTree++;
			if (sizeofTree == hmap.size()) {
				t.fin = true;
				lastP = entry.getKey();
				// System.out.println("last: "+lastP);
			}
			sequence = entry.getKey();
			t.seq = sequence;
			bytearray = entry.getValue();
			t.payload = (byte[]) bytearray;
			copyofhmap.put(seqincopy++, t);
			t = new TCP();

		}
		synchronized (a1) {
			a1.notify();

		}
		int k = 1;
		/*
		 * Sends first packet with cwnd(congestion Window:1) and waits for its
		 * acknowledgement.The packet includes payload,checksum,fin flagand
		 * sequence number.
		 */

		outer: while (true) {
           
			// Waiting for acknowledgement

			// for loop starts from the last correctly acknowledged packet
			// number till it is less than the
			// congestionWindow size.
			for (k = window; k < window + cwnd; k++) {
				if (k == lastP) {
					lastPSent = true;
				}
				if (k > lastP) {
					// System.out.println("Last Packet: "+lastP);
					break outer;
				}
				// System.out.println("hello");
				if (!congestionWindow.containsKey(k)) {
					Object seq1 = copyofhmap.keySet().toArray()[k];
					int noofsentP = (Integer) seq1;
					congestionWindow.put(noofsentP, 0);
					TCP tcp = copyofhmap.get(k);
					checksumPayload = tcp.payload;
					computedChecksum = ComputeChecksum(checksumPayload);
					tcp.checksum = computedChecksum;
					// System.out.println("Computed Check sum for: " + noofsentP
					// + " = " + computedChecksum);
					if (quiet == false) {
						System.out
								.println("------------------tcp packet sent-----------------------------------: "
										+ k);
					}
					outputStream = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(outputStream);
					os.writeObject(tcp);
					sendser = outputStream.toByteArray();
					sendPacket = new DatagramPacket(sendser, sendser.length,
							serAddress, port);
					// Thread.sleep(2);
					clntsock.send(sendPacket);

				}
			}
			synchronized (a) {
				a.wait();
			}

		}

	}

	/*
	 * Cmputes Checksum
	 */
	public static int ComputeChecksum(byte[] checksumPayload2)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(checksumPayload2, 0, checksumPayload2.length);
		BigInteger check = new BigInteger(1, md.digest());
		return check.intValue();
	}

	/**
	 * Receives acknowledgement from the server within a time period.If it does
	 * not receive anything till the timeout that particular packet is
	 * retransmitted.Increases the size of the congestion Window(cwnd) by 1 for
	 * each correctly received acknowledgement and till cwnd<sstresh. if cwnd >
	 * sstresh then cwnd=cwnd+1/cwnd. After retransmission and timeout we set
	 * sstreah=cwnd/2 and cwnd=1;
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws InterruptedException
	 */
	public void ClientReceiver() throws IOException, ClassNotFoundException,
			NoSuchAlgorithmException, InterruptedException {
		synchronized (a1) {
			a1.wait();
		}

		float temp = 0;
		boolean timeoutFlag = false;
		byte[] clntrec = new byte[2000];
		// clntreceive = new DatagramSocket(1029);
		while (true) {
			try {

				receivep = new DatagramPacket(clntrec, clntrec.length);
				UDPClient.clntsock.setSoTimeout(timeout);
				UDPClient.clntsock.receive(receivep);
				// UDPClient.clntsock.setSoTimeout(timeout);
				byte[] data = receivep.getData();
				ByteArrayInputStream in = new ByteArrayInputStream(data);

				ObjectInputStream is = null;
				try {
					is = new ObjectInputStream(in);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					Ack t1 = (Ack) is.readObject();
					if (quiet == false) {
						System.out.println("CuurentAck: " + t1.ack);
					}
					if (t1.ack > lastP) {
						break;
					}

					if (previousAck == t1.ack) {

					} else {
						window = t1.ack;
						countertill3 = 0;
					}
					if (previousAck == t1.ack && previousAck != retransmitted) {

						countertill3++;
						if (quiet == false) {
							System.out.println("Duplicate Acknowledgement: "
									+ countertill3);
						}
						if (countertill3 == 3) {
							if (previousAck == retransmitted) {
								countertill3 = 0;
							} else {
								sstresh = cwnd / 2;
								cwnd = 1;
								retrans(previousAck);
								retransmitted = previousAck;
								if (quiet == false) {
									System.out.println("-------"
											+ "RETRANSMITT" + "--------");
								}
								countertill3 = 0;

							}
						}

					}

					if (cwnd < sstresh) {
						if (previousAck != t1.ack) {
							cwnd++;
						}

					} else {

						if (previousAck != t1.ack) {
							temp = temp + (1 / cwnd);
							if (temp == 1) {
								cwnd = cwnd + 1;
								temp = 0;
							}
						}

					}
					previousAck = t1.ack;

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (Exception e) {
				// if(lastPSent==true){
				// break;
				// }
				countertill3 = 0;
				UDPClient.sstresh = cwnd / 2;
				UDPClient.cwnd = 1;
				if (quiet == false) {
					System.out.println("$$$$$$$$$$$$TIMEOUT$$$$$$$$$$  "
							+ previousAck);
				}
				UDPClient.retrans(previousAck);
				timeoutFlag = true;

			}

			synchronized (a) {
				a.notify();
			}
		}
	}

	public static void retrans(int seq) throws IOException,
			NoSuchAlgorithmException {
		countertill3 = 0;
		Object seq3 = copyofhmap.keySet().toArray()[seq];
		int noofsentP = (Integer) seq3;
		if (quiet == false) {
			System.out.println("Sequence number of packet retransmitted: "
					+ noofsentP);
		}
		// al.add(noofsentP);
		congestionWindow.put(noofsentP, 0);
		TCP tcp = copyofhmap.get(seq);
		checksumPayload = tcp.payload;
		computedChecksum = ComputeChecksum(checksumPayload);
		tcp.checksum = computedChecksum;
		// System.out.println("Computed Checksum of :  " + noofsentP + " = "
		// + computedChecksum);
		outputStream = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(outputStream);
		os.writeObject(tcp);
		sendser = outputStream.toByteArray();
		sendPacket = new DatagramPacket(sendser, sendser.length, serAddress,
				port);

		clntsock.send(sendPacket);
	}

	/**
	 * Checksum of the original file is computed.
	 * 
	 * @param file
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public void Checksum(String file) throws NoSuchAlgorithmException,
			IOException {
		int count1 = 0;
		byte[] accept1 = new byte[1460];
		File f = new File(file);
		FileInputStream fs = null;
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		try {
			fs = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while ((count1 = fs.read(accept1)) != -1) {
			md5.update(accept1, 0, count1);
		}
		byte[] MD5checksum = md5.digest();
		StringBuffer string = new StringBuffer("");
		for (int i = 0; i < MD5checksum.length; i++) {
			string.append(Integer.toString((MD5checksum[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		System.out.println("Checksum of entire file:  " + string);
	}
}

class TCP implements Serializable {
	public int seq;
	public int ack;
	public String s;
	boolean fin = false;
	// public long checksum;
	public byte[] payload = new byte[1460];
	int checksum;

	public TCP() {

	}

}