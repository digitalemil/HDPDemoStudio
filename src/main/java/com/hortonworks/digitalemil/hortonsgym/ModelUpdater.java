package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class ModelUpdater
 */
public class ModelUpdater extends HttpServlet {
	private static final long serialVersionUID = 1L;
	String pmml;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ModelUpdater() {
		super();
	}

	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Writer writer = response.getWriter();
				
		if (HortonsGym.isInSafeMode()) {
			pmml = HortonsGym.getModelString();
		} 
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this
					.getClass().getResourceAsStream("/model.html")));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("PMMLCONTENT")) {
					line = pmml;
					if(line== null)
						line= "";
				}
				writer.write(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		BufferedReader reader = request.getReader();
		StringBuffer in = new StringBuffer();

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			in.append(line);
		} while (true);

		if (HortonsGym.isInSafeMode()) {
			HortonsGym.setModelString(in.toString());
		} else {
			pmml= in.toString();
		}
	}

}
