package cs601.hotelapp;

import java.nio.file.Paths;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;

public class JettyHttpServer {
	//public static final int PORT = 8080;
	public static final int PORT = 3050;

	public static void main(String[] args) throws Exception {
		// Before we start our server, we need to load all the hotel data 
		// (both general hotel info and reviews) into our data structures 
		// from the input files.
		ThreadSafeHotelData tsData = new ThreadSafeHotelData();
		HotelDataBuilder hotelDataBuilder = new HotelDataBuilder(tsData);
		hotelDataBuilder.loadHotelInfo("input/hotels200.json");
		hotelDataBuilder.loadReviews(Paths.get("input/reviews"));
		hotelDataBuilder.shutdown();
		
		// Server	
		Server server = new Server(PORT);	
		
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setContextPath("/");
		servletContextHandler.addServlet(JettyHotelInfoServlet.class, "/hotelInfo");
		servletContextHandler.addServlet(JettyReviewsServlet.class, "/reviews");
		servletContextHandler.addServlet(JettyOthersServlet.class, "/");
		servletContextHandler.setAttribute("tsData", tsData);
		
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {servletContextHandler});
		
		server.setHandler(handlers);
		server.start();
		System.out.println("Server is ready!!!");
		server.join();
				
				
		/*
		// Server	
		Server server = new Server(PORT);		
		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(JettyHotelInfoServlet.class, "/hotelInfo");
		servletHandler.addServletWithMapping(JettyReviewsServlet.class, "/reviews");
		server.setHandler(servletHandler);
		server.start();
		System.out.println("Server is ready!!!");
		server.join();
		*/
	}

}
