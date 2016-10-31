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
public class JettyReviewsServlet extends HttpServlet {

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		
		PrintWriter printWriter = resp.getWriter();
		
		String hotelId = req.getParameter("hotelId");
		String numString = req.getParameter("num");
		if (hotelId == null || hotelId.isEmpty() || numString == null || numString.isEmpty()){
			printWriter.println("Something is wrong with Restful API.");
		} else {
			ThreadSafeHotelData tsData = (ThreadSafeHotelData) getServletContext().getAttribute("tsData");
			hotelId = StringEscapeUtils.escapeHtml4(hotelId);
			numString = StringEscapeUtils.escapeHtml4(numString);
			int num = Integer.parseInt(numString);
			int maxNum = tsData.maxNumberOfReviewsForHttpServer(hotelId);
			if(num > maxNum){ num = maxNum;	}			
			Hotel hotel = tsData.containsHotelKeyForHttpServer(hotelId.trim());
			if(hotel!= null) {
				printWriter.print(tsData.getJSONReviewsForHttpServer(hotelId, num));
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
