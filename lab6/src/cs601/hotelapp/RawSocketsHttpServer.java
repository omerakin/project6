package cs601.hotelapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import org.json.simple.JSONObject;
import cs601.concurrent.WorkQueue;

public class RawSocketsHttpServer extends Thread{
	//public final static int PORT = 8080;
	public final static int PORT = 3000;
	private final WorkQueue workQueue;
	private ThreadSafeHotelData tsData;
	private HotelDataBuilder hotelDataBuilder;
	private boolean alive;
	private volatile int numTasks; // how many runnable tasks are pending
	
	public RawSocketsHttpServer() {
		// Before we start our server, we need to load all the hotel data 
		// (both general hotel info and reviews) into our data structures 
		// from the input files.
		tsData = new ThreadSafeHotelData();
		hotelDataBuilder = new HotelDataBuilder(tsData);
		hotelDataBuilder.loadHotelInfo("input/hotels200.json");
		hotelDataBuilder.loadReviews(Paths.get("input/reviews"));
		hotelDataBuilder.shutdown();
		//Initialize other variable
		workQueue = new WorkQueue();
		alive = true;
		numTasks=0;
		System.out.println("Server is ready!!!");
	}
	
	public void run() {
		ServerSocket serverSocket = null;
		Socket socket = null;
		
		try {
			// For listening for connection request from clients
			serverSocket = new ServerSocket(PORT);
			while(alive){
				// Waits for a client to connect, creates a new connection socket for talking to this client.
				socket = serverSocket.accept();
				// WelcomingSocket will continue listening for connections from other clients
				// We can send this connection to WorkQueue
				workQueue.execute(new ServersWorker(socket));
			}			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (serverSocket != null && !serverSocket.isClosed())
					serverSocket.close();
				if (socket != null && !socket.isClosed())
					socket.close();
			} catch (IOException e) {
				System.out.println("Could not close the socket");
			}
		}
	}
	
	private class ServersWorker implements Runnable {
		private Socket socket;

		public ServersWorker(Socket socket) {
			this.socket = socket;
			incrementNumTasks();
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			BufferedReader bufferedReader = null;
			PrintWriter printWriter = null;
			try {
				bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				// The server can now read lines sent by the client using BufferedReader
				String input = new String();
				input = bufferedReader.readLine();
				System.out.println("Server received: " + input); // Server received: GET /hello HTTP/1.1
				// The server can parse lines sent by the client
				String[] requestParams = input.split(" ");
				// The server can send response if Http request is not Get.
				if(!requestParams[0].equals("GET")) {
					header405Message(printWriter);
					printWriter.println("405 Method not allowed!");
				} else {
					// check path
					if(requestParams[1].contains("?") && requestParams[1].contains("=")){
						String path = requestParams[1].substring(0, requestParams[1].indexOf("?"));
						//System.out.println(path);
						
						// /hotelInfo
						if(path.equals("/hotelInfo")){
							//System.out.println("in /hotelInfo");
							String parameter = requestParams[1].substring(requestParams[1].indexOf("?"));
							String[] parameters = parameter.split("=");
							if((parameters.length == 2) && parameters[0].equals("?hotelId")) {
								String hotelId = parameters[1];
								//System.out.println(hotelId);
								Hotel hotel = tsData.containsHotelKeyForHttpServer(parameters[1]);
								// check hotelId exist or not
								if(hotel != null) {								
									headerMessage(printWriter);
									// Json File
									JSONObject jsonObject = new JSONObject();
									jsonObject.put("success", true);
									jsonObject.put("hotelId", hotelId);
									jsonObject.put("name", hotel.getHotel_name());
									jsonObject.put("addr", hotel.getAddress().getStreet_address());
									jsonObject.put("city", hotel.getAddress().getCity());
									jsonObject.put("state", hotel.getAddress().getState());
									jsonObject.put("lat", hotel.getAddress().getLongitude());
									jsonObject.put("lng", hotel.getAddress().getLatitude());
									jsonObject.put("country", hotel.getAddress().getCountry());
									printWriter.print(jsonObject);
								} else {
									headerMessage(printWriter);
									JSONObject jsonObjectNotExist = new JSONObject();
									jsonObjectNotExist.put("success", false);
									jsonObjectNotExist.put("hotelId", "invalid");
									jsonObjectNotExist.writeJSONString(printWriter);						
								}
							} else {
								warningMessage(printWriter);
							}
						} else if (path.equals("/reviews")) {
							//System.out.println("in /reviews");
							String parameter = requestParams[1].substring(requestParams[1].indexOf("?"));
							String[] parameters = parameter.split("&");
							if(parameters.length == 2) {
								String[] hotelIdPart = parameters[0].split("=");
								String[] numPart = parameters[1].split("=");
								if((hotelIdPart.length == 2) && (hotelIdPart[0].equals("?hotelId"))
										&& (numPart.length == 2) && (numPart[0].equals("num"))) {
									int num = Integer.parseInt(numPart[1]);
									int maxNum = tsData.maxNumberOfReviewsForHttpServer(hotelIdPart[1]);
									if(num > maxNum){ num = maxNum;	}
									Hotel hotel = tsData.containsHotelKeyForHttpServer(hotelIdPart[1]);
									// check hotelId exist or not
									if(hotel!= null) {								
										headerMessage(printWriter);
										printWriter.print(tsData.getJSONReviewsForHttpServer(hotelIdPart[1], num));	
									} else {
										headerMessage(printWriter);
										JSONObject jsonObjectNotExist = new JSONObject();
										jsonObjectNotExist.put("success", false);
										jsonObjectNotExist.put("hotelId", "invalid");
										jsonObjectNotExist.writeJSONString(printWriter);			
									}									
								} else {
									warningMessage(printWriter);
								}
							} else {
								warningMessage(printWriter);
							}
						} else {
							warningMessage(printWriter);
						}
					} else {
						if (requestParams[1].contains("hotelInfo") || requestParams[1].contains("reviews")){
							headerMessage(printWriter);
							JSONObject jsonObjectNotExist = new JSONObject();
							jsonObjectNotExist.put("success", false);
							jsonObjectNotExist.put("hotelId", "invalid");
							jsonObjectNotExist.writeJSONString(printWriter);
						} else {
							header404Message(printWriter);
						}						
					}	
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					printWriter.close();
					bufferedReader.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					decrementNumTask();
				}
			}
		}
	}
	
	/**
	 * Generalise of warning message
	 * @param printWriter
	 * 				-	Output printWriter
	 */
	@SuppressWarnings("unchecked")
	public synchronized void warningMessage(PrintWriter printWriter) {
		headerMessage(printWriter);
		JSONObject jsonObjectNotExist = new JSONObject();
		jsonObjectNotExist.put("success", false);
		jsonObjectNotExist.put("hotelId", "invalid");
		try {
			jsonObjectNotExist.writeJSONString(printWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Header of output
	 * @param printWriter 
	 * 				-	Output printWriter
	 */
	public synchronized void headerMessage(PrintWriter printWriter) {
		// Header
		printWriter.println("HTTP/1.1 200 OK");
		printWriter.println("Content-Type: application/json");
		printWriter.println("Server: AKIN");
		printWriter.println(""); // This blank line signals the end of the headers
	}
	
	/**
	 * Header of output
	 * @param printWriter 
	 * 				-	Output printWriter
	 */
	public synchronized void header404Message(PrintWriter printWriter) {
		// Header
		printWriter.println("HTTP/1.1 404 Not Found");
		printWriter.println("Content-Type: application/json");
		printWriter.println("Server: AKIN");
		printWriter.println(""); // This blank line signals the end of the headers
	}
	
	/**
	 * Header of output
	 * @param printWriter 
	 * 				-	Output printWriter
	 */
	public synchronized void header405Message(PrintWriter printWriter) {
		// Header
		printWriter.println("HTTP/1.1 405 Method Not Allowed");
		printWriter.println("Content-Type: application/json");
		printWriter.println("Server: AKIN");
		printWriter.println(""); // This blank line signals the end of the headers
	}
	
	/**
	 *  Wait for all pending work to finish
	 */
	public synchronized void waitUntilFinished() {
		while(numTasks > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * increment number of task in synchronised way
	 */
	public synchronized void incrementNumTasks() {
		numTasks++;
	}

	/**
	 * decrement number of task in synchronised way
	 */
	public synchronized void decrementNumTask() {
		numTasks--;
		if(numTasks <= 0){
			notifyAll();
		}
	}

	/**
	 * Wait until there is no pending work, then shutdown the queue
	 */
	public synchronized void shutdown(){
		waitUntilFinished();
		workQueue.shutdown();
	}
	
	public static void main(String[] args) {		
		RawSocketsHttpServer server = new RawSocketsHttpServer();
		server.start();
		try {
			server.join();
			server.shutdown();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException occurred " + e);
		}
	}
}
