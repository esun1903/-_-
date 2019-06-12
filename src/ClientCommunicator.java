import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientCommunicator extends Thread {
	private PrintWriter out;		// to server
	private BufferedReader in;		// from server
	private 오목 client;
	private Socket sock;

	public ClientCommunicator(String serverIP, 오목 client) {
		this.client = client;
		System.out.println("connecting to " + serverIP + "...");
		try {
			sock = new Socket(serverIP, 8080);
			out = new PrintWriter(sock.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			System.out.println("...connected");
		}
		catch (IOException e) {
			System.err.println("couldn't connect");
			System.exit(-1);
		}
	}

	// send message to server
	public void send(String msg) {
		out.println(msg);
	}

	public void run() {
		String line;
		try {
			while ((line = in.readLine()) != null) {
				String[] part = line.split(" ");
				switch (part[0]) {
					case "add":
						client.getPieces().add(new Point(Integer.parseInt(part[1]), Integer.parseInt(part[2])));
						client.setShow(client.getPieces().size());
						client.checkWin();
						client.getContentPane().repaint();
						break;
					case "undo":
						client.getPieces().remove(client.getPieces().size() - 1);
						client.incrementUndo(client.getPieces().size() % 2);
						client.setShow(client.getPieces().size());
						client.getContentPane().repaint();
						break;
					default:  // connected
						client.setConnecting(false);
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("server hung up");
			try {
				// cleanup
				out.close();
				in.close();
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}