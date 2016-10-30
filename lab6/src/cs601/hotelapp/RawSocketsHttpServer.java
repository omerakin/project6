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
	public final static int PORT = 8080;
	private static final String TITLE = "RawSocketsHttpServer";
	private final WorkQueue workQueue;
	private ThreadSafeHotelData tsData;
	private HotelDataBuilder hotelDataBuilder;
	
	public RawSocketsHttpServer() {
		// Before we start our server, we need to load all the hotel data 
		// (both general hotel info and reviews) into our data structures 
		// from the input files.
		tsData = new ThreadSafeHotelData();
		hotelDataBuilder = new HotelDataBuilder(tsData);
		hotelDataBuilder.loadHotelInfo("input/hotels200.json");
		hotelDataBuilder.loadReviews(Paths.get("input/reviews8000"));
		hotelDataBuilder.shutdown();
		workQueue = new WorkQueue();
		System.out.println("Server is ready!!!");
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
		} finally {
			/*
			try {
				if (serverSocket != null && !serverSocket.isClosed())
					serverSocket.close();
				if (socket != null && !socket.isClosed())
					socket.close();
			} catch (IOException e) {
				System.out.println("Could not close the socket");
			}
			*/
		}
	}
	
	private class ServersWorker implements Runnable {
		private Socket socket;

		public ServersWorker(Socket socket) {
			this.socket = socket;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				// The server can now read lines sent by the client using BufferedReader
				String input = new String();
				input = bufferedReader.readLine();
				System.out.println("Server received: " + input); // Server received: GET /hello HTTP/1.1
				// The server can parse lines sent by the client
				String[] requestParams = input.split(" ");
				// The server can send response if Http request is not Get.
				if(!requestParams[0].equals("GET")) {
					headerMessage(printWriter);
					htmlHeaderMessage(printWriter);
					printWriter.println("<p> 405 Method not allowed! </p>");
					htmlFooterMessage(printWriter);
				} else {
					// check path
					if(requestParams[1].contains("?") && requestParams[1].contains("=")){
						String path = requestParams[1].substring(0, requestParams[1].indexOf("?"));
						System.out.println(path);
						
						// /hotelInfo
						if(path.equals("/hotelInfo")){
							System.out.println("in /hotelInfo");
							String parameter = requestParams[1].substring(requestParams[1].indexOf("?"));
							String[] parameters = parameter.split("=");
							if((parameters.length == 2) && parameters[0].equals("?hotelId")) {
								String hotelId = parameters[1];
								System.out.println(hotelId);
								Hotel hotel = tsData.containsHotelKeyForHttpServer(parameters[1]);
								// check hotelId exist or not
								if(hotel != null) {								
									headerMessage(printWriter);
									htmlHeaderMessage(printWriter);
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
									jsonObject.put("country", "USA .... NEED TO BE INVOKED");
									printWriter.print(jsonObject);
									htmlFooterMessage(printWriter);	
								} else {
									headerMessage(printWriter);
									htmlHeaderMessage(printWriter);
									JSONObject jsonObjectNotExist = new JSONObject();
									jsonObjectNotExist.put("success", false);
									jsonObjectNotExist.put("hotelId", "invalid");
									jsonObjectNotExist.writeJSONString(printWriter);
									htmlFooterMessage(printWriter);									
								}
							} else {
								warningMessage(printWriter);
							}
						} else if (path.equals("/reviews")) {
							System.out.println("in /reviews");
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
										htmlHeaderMessage(printWriter);
										printWriter.print(tsData.getJSONReviewsForHttpServer(hotelIdPart[1], num));
										htmlFooterMessage(printWriter);	
									} else {
										headerMessage(printWriter);
										htmlHeaderMessage(printWriter);
										JSONObject jsonObjectNotExist = new JSONObject();
										jsonObjectNotExist.put("success", false);
										jsonObjectNotExist.put("hotelId", "invalid");
										jsonObjectNotExist.writeJSONString(printWriter);
										htmlFooterMessage(printWriter);									
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
						warningMessage(printWriter);
					}	
				}
				bufferedReader.close();
				printWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Generalise of warning message
	 * @param printWriter
	 * 				-	Output printWriter
	 */
	public synchronized void warningMessage(PrintWriter printWriter) {
		headerMessage(printWriter);
		htmlHeaderMessage(printWriter);
		printWriter.println("<p> Something is wrong with Restful API. </p>");
		htmlFooterMessage(printWriter);
	}
	
	/**
	 * Header of output
	 * @param printWriter 
	 * 				-	Output printWriter
	 */
	public synchronized void headerMessage(PrintWriter printWriter) {
		// Header
		printWriter.println("HTTP/1.0 200 OK");
		printWriter.println("Content-Type: text/html");
		printWriter.println("Server: AKIN");
		printWriter.println(""); // This blank line signals the end of the headers
	}
	
	/**
	 * HTML(Header) of output
	 * @param printWriter
	 * 				-	Output printWriter
	 */
	public synchronized void htmlHeaderMessage(PrintWriter printWriter) {
		// HTML(Header) page
		printWriter.println("<!DOCTYPE HTML>");
		printWriter.println("<html><head>");
		printWriter.println("\t<title>" + TITLE + "</title>");
		printWriter.println("<content=\"text/html;charset=utf-8\">");
		printWriter.println("</head>");
		printWriter.println("<body>");
	}
	
	/**
	 * HTML(Footer) of output
	 * @param printWriter
	 * 				-	Output printWriter
	 */
	public synchronized void htmlFooterMessage(PrintWriter printWriter) {
		// HTML(Footer) page
		printWriter.println("</body>");
		printWriter.println("</html>");
		printWriter.flush();		
	}
	
	public static void main(String[] args) {		
		RawSocketsHttpServer server = new RawSocketsHttpServer();
		server.start();
		try {
			server.join();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException occurred " + e);
		}
	}
}
