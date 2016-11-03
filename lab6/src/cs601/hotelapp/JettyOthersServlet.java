package cs601.hotelapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class JettyOthersServlet extends HttpServlet{

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		PrintWriter printWriter = resp.getWriter();
		JSONObject jsonObjectNotExist = new JSONObject();
		jsonObjectNotExist.put("success", false);
		jsonObjectNotExist.put("hotelId", "invalid");
		jsonObjectNotExist.writeJSONString(printWriter);
		resp.setStatus(HttpServletResponse.SC_OK);		
	}
	
	

}
