import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes two threads simultaneously.Receiving thread accepts array of bytes
 * along with the header information from the client and sender thread sends
 * acknowledgement for each accepted packet.
 * 
 * @author Anjali
 *
 */
public class UDPServer extends Thread {
	int port;
	static DatagramPacket receivep;
	static InetAddress clntAddress;
	static DatagramPacket sendp;
	private int id = 0;
	static byte[] accept = new byte[2000];
	byte[] sendclnt = new byte[2000];
	byte[] p = new byte[2000];
	static byte[] checksumPayload = new byte[1460];
	static Vector handler = new Vector(10);
	static Vector a = new Vector();
	static int port1;
	static boolean flag;
	static TCP tcpSer;
	static int ack;
	static int acknowledgement,checkforFinal;
	static DatagramSocket sersock = null;
	static DatagramPacket serack;
	ByteArrayOutputStream bos;
	ObjectOutputStream os;
	static TreeMap<Integer, byte[]> tm;
	static TreeMap<Integer, Boolean> keepTrack;
	static boolean sendflag = false, fl, seenSeq, temp, lastPacket = false;
	static int expectedPacket, seqinTree, correctly_received_packets,
			prevSeq = 1, lastReceivedPacket;
	byte[] bytearray = new byte[1460];
	static boolean quiet = false;
	static int computedChecksum, min, minAck;

	/**
	 * 
	 * @param port
	 *            :Port number at which the server is listening.
	 * @param quiet
	 *            : If quiet is true only checksum of the entire file is
	 *            calculated.
	 */
	UDPServer(int port, boolean quiet) {
		this.port = port;
		this.quiet = quiet;
	}

	/**
	 * if id =1 ServerReceiver is started else SeerverSender is started.
	 */
	public void run() {
		synchronized (handler) {
			handler.addElement(this);
			id = handler.size();
		}
		if (id == 1) {
			try {
				ServerReceiver();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {

			try {
				ServerSender();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * ServerReceiver accepts packets from the Client and buffers it.Two
	 * TreeMaps are maintained to keep track of the packets received and one to
	 * buffer the incoming data along with its sequence number. It then sends an
	 * acknowledgement to the client of the next expected packet.
	 * 
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void ServerReceiver() throws ClassNotFoundException,
			NoSuchAlgorithmException, IOException, InterruptedException {
		tm = new TreeMap<Integer, byte[]>();
		keepTrack = new TreeMap<Integer, Boolean>();
		long checksum = 0;
		int count1 = 0;

		try {
			sersock = new DatagramSocket(port);
		} catch (SocketException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		while (true) {
			receivep = new DatagramPacket(accept, accept.length);

			try {
				sersock.receive(receivep);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			byte[] data = receivep.getData();
			// System.out.println(data);
			ByteArrayInputStream in = new ByteArrayInputStream(data);

			ObjectInputStream is = null;
			try {
				is = new ObjectInputStream(in);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {

				tcpSer = (TCP) is.readObject();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			checksumPayload = tcpSer.payload;
			computedChecksum = ComputeChecksum(checksumPayload);

			if (tcpSer.checksum == computedChecksum) {

				// System.out.println("Computed Checksum for:  " + tcpSer.seq
				// + " = " + computedChecksum);

				// seqinTree = tcpSer.seq;
				// System.out.println("NEW SEQ: "+seqinTree);
				tm.put(tcpSer.seq, tcpSer.payload);
				
				
				keepTrack.put(tcpSer.seq, true);
				if (!keepTrack.containsKey(tcpSer.seq + 1)) {
					keepTrack.put(tcpSer.seq + 1, false);
				}
				if (quiet == false) {
					System.out.println("Packet Expected: " + expectedPacket);
					System.out.println("Packet Arrived: " + tcpSer.seq);
				}
				if (expectedPacket == tcpSer.seq) {
					if (quiet == false) {
						System.out.println("Packet Received  " + tcpSer.seq);
						
					}
					if (keepTrack.get(tcpSer.seq + 1).equals(false)) {
						acknowledgement = tcpSer.seq + 1;
						expectedPacket = acknowledgement;
					} else {
						if (quiet == false) {
							System.out
									.println("Packet Received  " + tcpSer.seq);
						}
						min = 99999999;
						for (Entry<Integer, Boolean> entry : keepTrack
								.entrySet()) {
							if (entry.getKey() < min) {
								if (entry.getValue().equals(false)) {
									min = entry.getKey();
									minAck = min;
								}
							}
						}
						acknowledgement = minAck;
						expectedPacket = acknowledgement;
					}

				} else {
					if (quiet == false) {
						System.out.println("Packet Received  " + tcpSer.seq);
					}
					min = 99999999;
					for (Entry<Integer, Boolean> entry : keepTrack.entrySet()) {
						if (entry.getKey() < min) {
							if (entry.getValue().equals(false)) {
								min = entry.getKey();
								minAck = min;
								// System.out.println("MinAck: "+minAck);
							}
						}
					}
					acknowledgement = minAck;
					expectedPacket = acknowledgement;
					// if(keepTrack.containsKey(seq))
				}

			} else {
				System.out.println("Checksum not passed  ");
			}

			clntAddress = receivep.getAddress();
			port1 = receivep.getPort();

			// Notifies Sender thread to send an acknowledgement
			synchronized (a) {
				a.notify();
			}
			// Checks for the last packet.If last packet is received sets
			// sendflag to
			// to true.Waits till all the packets are received and then breaks
			// out of the while loop.
			if (tcpSer.fin == true) {
				lastReceivedPacket = tcpSer.seq;
				sendflag = true;
				System.out.println("SendFlag=true");
				checkforFinal = lastReceivedPacket + 1;
				FileOutputStream fos = new FileOutputStream("fil3.txt");
				for (int i = 0; i < tm.size(); i++) {
					if (tm.get(i) != null) {
						// System.out.print(new String(tm.get(i)));
						fos.write(tm.get(i));

					}
				}

				fos.close();
				System.out.println("Checksum After Last Packet received");
				Checksum("fil3.txt");
			}
			if (sendflag == true) {
				
				if (expectedPacket == checkforFinal) {
					acknowledgement = expectedPacket;
					lastPacket = true;
				}
			}
			if (lastPacket == true) {
				FileOutputStream fos = new FileOutputStream("fil3.txt");
				for (int i = 0; i < tm.size(); i++) {
					if (tm.get(i) != null) {
						// System.out.print(new String(tm.get(i)));
						fos.write(tm.get(i));

					}
				}

				fos.close();
				System.out.println("Checksum after all the packets are received");
				Checksum("fil3.txt");
				break;
			}
		}
	}

	/**
	 * Checksum of each payload is calculated
	 * 
	 * @param checksumPayload2
	 *            : Payload
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static int ComputeChecksum(byte[] checksumPayload2)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(checksumPayload2, 0, checksumPayload2.length);
		BigInteger check = new BigInteger(1, md.digest());
		return check.intValue();
	}

	/**
	 * ServerSender waits till the ServerReceiver notifies it to send
	 * acknowledgement to the client.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public void ServerSender() throws InterruptedException, IOException,
			NoSuchAlgorithmException {
		Ack ak = new Ack();
		ArrayList cal = new ArrayList();

		while (true) {
			if (lastPacket == true) {
//				System.out.println("Sending Thread");
//				FileOutputStream fos = new FileOutputStream("fil3.txt");
//				for (int i = 0; i < tm.size(); i++) {
//					if (tm.get(i) != null) {
//						// System.out.print(new String(tm.get(i)));
//						fos.write(tm.get(i));
//
//					}
//				}
//
//				fos.close();
//				Checksum("fil3.txt");

				break;
				// a.wait();
			} 
			// synchronized (a) {
			// System.out.println("IN WAIT:");
			
			// System.out.println("AFTER WAIT:");
			// for(int i=0;i<al.size();i++){
			synchronized (a) {
				a.wait();
			}
			ak.ack = acknowledgement;
			// al.remove(i);

			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);

			// tcpSer.payload=(byte[])arr;
			
			if (quiet == false) {
				System.out.println("Ak data: " + ak.ack);
			}
			os.writeObject(ak);
			byte[] sendclnt1 = bos.toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(sendclnt1,
					sendclnt1.length, clntAddress, port1);

			sersock.send(sendPacket);
			

		}

	}

	/**
	 * Checksum of the reconstructed file is computed.
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

class Ack implements Serializable {
	int ack;

	public Ack() {
		ack = this.ack;

	}
}
