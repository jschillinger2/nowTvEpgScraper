package org.nowTvChannelScraper.main;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Downloads the programme info from NowTV website and saves it to a MythTV
 * compatible XML file. <br/>
 * Such file should be read in using "mythfilldatabase --file 1 filename.xml"
 * 
 * @author Julian Schillinger
 * 
 */
@SuppressWarnings("deprecation")
public class Main {

	private static final String[] URLS = {
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G01.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G02.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G03.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G04.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G05.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G06.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G07.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G08.json",
			"http://nowtv.now.com/gw-epg/epg/en_us/<DATE>/prf0/resp-genre/ch_G09.json" };

	/**
	 * How many days in the future to get the programme of in addition to
	 * today's programme.
	 */
	final static int DAYS_IN_FUTURE = 4;

	public static void main(String[] args) throws ClientProtocolException,
			IOException, ParseException {

		/*
		 * Init
		 */

		// init data model
		ArrayList<Program> programsOut = new ArrayList<Program>();
		ArrayList<String> channels = new ArrayList<String>();

		// get command line args
		if (args.length < 1 || !args[args.length - 1].contains(".xml"))
			throw new RuntimeException(
					"Please add output filename (ending with .xml) to command line args.");
		String outputFilename = args[args.length - 1];

		/*
		 * Read programmes from NowTV website and add to data model.
		 */
		Date today = new Date();
		for (int dayOffset = 0; dayOffset <= DAYS_IN_FUTURE; dayOffset++) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String dateStr = sdf.format(today);
			for (String url : URLS) {
				String urlWithDate = url.replace("<DATE>", dateStr);
				readProgrammes(urlWithDate, programsOut, channels);
			}
			today.setDate(today.getDate() + 1);
		}

		/*
		 * Creates the XML code for MythTv and saves it to a file.
		 */
		createMythTvXml(outputFilename, programsOut, channels);
		System.out.println("Done!");
	}

	/**
	 * Creates the XML code for MythTv and writes it to given filename.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	private static void createMythTvXml(String outputFilename,
			ArrayList<Program> programsOut, ArrayList<String> channels)
			throws FileNotFoundException, UnsupportedEncodingException {

		// init
		PrintWriter writer = new PrintWriter(outputFilename, "UTF-8");
		System.out
				.println("Converting programme info to MythTV file: \n\tchannels="
						+ channels.size()
						+ "\n\tprogrammes="
						+ programsOut.size() + "\n\tfilename=" + outputFilename);

		// write xml - header
		writer.println("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
		writer.println("<tv generator-info-name=\"NowTVGenerator\" generator-info-url=\"http://www.example.com/\">\n");

		// write - channels
		for (String channel : channels) {
			writer.println("<channel id=\"" + channel + "\">");
			writer.println("<display-name lang=\"en\">" + channel
					+ "</display-name>");
			writer.println("<display-name lang=\"en\">" + channel
					+ "</display-name>");
			writer.println("<url>http://www.example.com</url>");
			writer.println("</channel>\n");
		}

		// write programmes
		int cntTotal = 0;
		int cntInterval = 0;
		for (Program program : programsOut) {

			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
			String startTimeStr = sdf.format(program.startTime);
			String endTimeStr = sdf.format(program.endTime);

			writer.println("<programme start=\"" + startTimeStr
					+ " +0800\" stop=\"" + endTimeStr + " +0800\" channel=\""
					+ program.channel + "\">");
			writer.println("<title lang=\"en\">"
					+ StringEscapeUtils.escapeXml(program.name) + "</title>");
			writer.println("<sub-title lang=\"en\"></sub-title>");
			writer.println("<desc>"
					+ StringEscapeUtils.escapeXml(program.description)
					+ "</desc>");
			writer.println("</programme>\n");
			if (cntInterval > 500) {
				System.out.println("Processed " + cntTotal + " programmes.");
				cntInterval = 0;
			}
			cntInterval++;
			cntTotal++;
		}

		// write footer
		writer.println("</tv>\n");
		writer.close();
	}

	/**
	 * Retrieve the JSON file from NowTV website, parses it and adds the info to
	 * the data model.
	 * 
	 * @param programsOut
	 *            An array list with programmes where the read programmes fill
	 *            be added to.
	 * @param channels
	 *            An array list with channels where the read channels will be
	 *            added to.
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ParseException
	 */
	@SuppressWarnings("resource")
	private static void readProgrammes(String url,
			ArrayList<Program> programsOut, ArrayList<String> channels)
			throws IOException, ClientProtocolException, ParseException {

		/*
		 * Get JSON from Now TV Website
		 */

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		System.out.println("Getting JSON from NowTV " + httpget.getURI());
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = httpclient.execute(httpget, responseHandler);

		/*
		 * Parse JSON
		 */

		// init
		JSONParser parser = new JSONParser();
		System.out.println("Parsing JSON");

		// parse header
		Object obj = parser.parse(responseBody);
		JSONObject jsonObject = (JSONObject) obj;
		JSONObject data = (JSONObject) jsonObject.get("data");
		JSONObject chProgram = (JSONObject) data.get("chProgram");

		// loop through channels
		for (Object channelObj : chProgram.keySet()) {
			String channel = (String) channelObj;
			JSONArray programs = (JSONArray) chProgram.get(channelObj);

			// loop through programmes
			for (Object programObj : programs.toArray()) {
				if (programObj instanceof JSONObject) {

					// init
					Program programOut = new Program();
					programOut.channel = channel;
					JSONObject program = (JSONObject) programObj;
					if (!channels.contains(channel))
						channels.add(channel);

					// end time
					Long startTime = (Long) program.get("start");
					Date startTimeDate = new Date(startTime);
					programOut.startTime = startTimeDate;

					// end time
					Long endTime = (Long) program.get("end");
					Date endTimeDate = new Date(endTime);
					programOut.endTime = endTimeDate;

					// name
					String name = (String) program.get("name");
					programOut.name = name;

					// description
					String description = (String) program.get("synopsis");
					programOut.description = description;

					// add to data object
					programsOut.add(programOut);
				}
			}
		}
	}
}
