package com.ibm.bluemix.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.nosql.json.api.BasicDBList;
import com.ibm.nosql.json.api.BasicDBObject;
import com.ibm.nosql.json.util.JSON;

/**
 * Utility class to send mail using SendGrid service in Bluemix
 * 
 * @author SUDDUTT1
 * 
 */
public class SendGridUtil {

	private static final Logger LOGGER = Logger.getLogger(SendGridUtil.class
			.getName());

	private static String apiURL;
	private static String userId;
	private static String userPassword;
	private static boolean isInitialized = false;

	/**
	 * Constructor made private to ensure no instance created.
	 */
	private SendGridUtil() {

	}

	/**
	 * Initialization code for internal calls by other public send mail methods
	 * 
	 * @return true if initialization is successful, false other wise
	 */
	private static boolean initialize() {
		try {
			if (!isInitialized) {

				String envServices = System.getenv("VCAP_SERVICES");
				if (envServices == null) {
					apiURL = "https://api.sendgrid.com/api/mail.send.json";
					// TODO:Change the user id and password by looking into the
					// SendGrid VCAP_SERVICES for testing in local
					userId = "";
					userPassword = "";
					isInitialized = true;
				} else {

					BasicDBObject obj = (BasicDBObject) JSON.parse(envServices);
					String thekey = null;
					Set<String> keys = obj.keySet();
					for (String eachkey : keys) {
						LOGGER.log(Level.INFO,
								"|SEND_GRID_UTIL| Matching the VCAP_SERVICES key :"
										+ eachkey);
						if (eachkey
								.contains(Constants.SENDGRID_SERVICE_LABEL_NAME)) {
							thekey = eachkey;
							break;
						}
					}
					if (thekey == null) {
						LOGGER.log(
								Level.WARNING,
								"|SEND_GRID_UTIL|Unable to find any Send grid service in the VCAP_SERVICES; Exiting");
						return false;
					} else {
						BasicDBList list = (BasicDBList) obj.get(thekey);
						obj = (BasicDBObject) list.get("0");
						LOGGER.log(
								Level.INFO,
								"|SEND_GRID_UTIL|Service found: "
										+ obj.get(Constants.SENDGRID_SERVICE_NAME_KEY));
						// Reading the credentials part
						obj = (BasicDBObject) obj
								.get(Constants.SENDGRID_SERVICE_CREDENTIAL_KEY);
						apiURL = Constants.SENDGRID_SERVICE_JSON_API_ENDPOINT;
						userPassword = (String) obj
								.get(Constants.SENDGRID_SERVICE_PASSWORD_KEY);
						userId = (String) obj
								.get(Constants.SENDGRID_SERVICE_USERNAME_KEY);
						isInitialized = true;
						LOGGER.log(Level.INFO,
								"|SEND_GRID_UTIL|SendGrid URL found : "
										+ apiURL);
						LOGGER.log(Level.INFO,
								"|SEND_GRID_UTIL|SendGrid Userid found : "
										+ userId);
						LOGGER.log(Level.INFO,
								"|SEND_GRID_UTIL|SendGrid Password found : "
										+ userPassword);
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING,
					"|SEND_GRID_UTIL| Exception thron during initialization",
					ex);
			isInitialized = false;
		}
		return isInitialized;
	}

	/**
	 * Sends a text mail to using SendGrid service
	 * @param fromMail String ( Sender's mail id )
	 * @param toEmail String  ( Receiver's mail id)
	 * @param subject String  ( Subject line)
	 * @param body String ( Mail body)
	 * @return true if mail sending successful , false otherwise.
	 */
	public static boolean sendMail(String fromMail, String toEmail,
			String subject, String body) {

		boolean isSuccess = false;
		String line = null;
		String finalResponse = null;
		try {

			if (initialize()) {

				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(apiURL);
				List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(
						10);
				nameValuePairs.add(new BasicNameValuePair("api_user", userId));
				nameValuePairs.add(new BasicNameValuePair("api_key",
						userPassword));
				nameValuePairs.add(new BasicNameValuePair("to", toEmail));
				nameValuePairs.add(new BasicNameValuePair("subject", subject));
				nameValuePairs.add(new BasicNameValuePair("text", body));
				nameValuePairs.add(new BasicNameValuePair("from", fromMail));
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = client.execute(post);
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				StringBuilder responseString = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					responseString.append(line);
				}
				finalResponse = responseString.toString();

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
						&& finalResponse != null
						&& finalResponse.contains("success")) {
					LOGGER.log(Level.INFO,
							"|SEND_GRID_UTIL|SendMail is successful "
									+ finalResponse);
					isSuccess = true;
				} else {
					LOGGER.log(Level.INFO,
							"|SEND_GRID_UTIL|SendMail is not successful "
									+ finalResponse);
					isSuccess = false;
				}
			} else {
				LOGGER.log(Level.WARNING,
						"|SEND_GRID_UTIL|Initialization is not successful ");
				isSuccess = false;
			}
		} catch (Exception ex) {
			LOGGER.log(Level.INFO,
					"|SEND_GRID_UTIL|SendMail exception thrown  ", ex);
			isSuccess = false;

		}
		return isSuccess;
	}
}
