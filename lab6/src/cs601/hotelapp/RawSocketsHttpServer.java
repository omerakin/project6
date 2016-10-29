package cs601.hotelapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import cs601.concurrent.WorkQueue;

public class RawSocketsHttpServer extends Thread{
	public final static int PORT = 8080;
	private final WorkQueue workQueue;
	
	
	public RawSocketsHttpServer() {
		workQueue = new WorkQueue();
	}
	
	public void run() {
		ServerSocket serverSocket = null;
		Socket socket = null;
		
		try {
			// For listening for connection request from clients
			serverSocket = new ServerSocket(PORT);
			// Waits for a client to connect, creates a new connection socket for talking to this client.
			socket = serverSocket.accept();
			// WelcomingSocket will continue listening for connections from other clients
			// We can send this connection to WorkQueue
			workQueue.execute(new ServersWorker(socket));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class ServersWorker implements Runnable {
		private Socket socket;

		public ServersWorker(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				// The server can now read lines sent by the client using BufferedReader
				String input = new String();
				input = bufferedReader.readLine();
				System.out.println("Server received: " + input);
				// The server can parse lines sent by the client
				//......
				// The server can send response if Http request is not Get.
				PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				
				//......
				
				bufferedReader.close();
				printWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	

}
