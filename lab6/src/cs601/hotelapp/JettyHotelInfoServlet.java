package cs601.hotelapp;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class JettyHotelInfoServlet extends HttpServlet{

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		
		PrintWriter printWriter = resp.getWriter();
		
		String hotelId = req.getParameter("hotelId");
		if (hotelId == null || hotelId.isEmpty()){
			printWriter.println("Something is wrong with Restful API.");
		} else {
			hotelId = StringEscapeUtils.escapeHtml4(hotelId);
			ThreadSafeHotelData tsData = (ThreadSafeHotelData) getServletContext().getAttribute("tsData");
			Hotel hotel = tsData.containsHotelKeyForHttpServer(hotelId.trim());
			if(hotel != null) {
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
				jsonObject.put("country", "USA");
				printWriter.print(jsonObject);
			} else {
				JSONObject jsonObjectNotExist = new JSONObject();
				jsonObjectNotExist.put("success", false);
				jsonObjectNotExist.put("hotelId", "invalid");
				jsonObjectNotExist.writeJSONString(printWriter);					
			}
		}
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	
}
