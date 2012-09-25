/*
 * Copyright (C) 2012 ZONE Media GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Implementation for pixlinQ mobile visual search
 * http://www.pixlinq.com/
 * 
 * setDelegate() and setDatasetID() have to be called before issuing a query!
 */

package at.zone.madeleine.ir;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.AsyncTask;
import android.os.Build;

public class PixlinqSearch implements IRService {

	private class PixlinqRequest {
		public String apiKey;
		public byte[] queryImage;
	}
	
	private String apiKey;
	private IRCallback delegate;
	
	private String lastRawRespose;
	private String deviceID = "DEVICE_ID_NOT_PROVIDED"; // used to identify device inside pixlinQ CMS
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - A C C E S S O R S
	public void setDelegate(IRCallback delegate) {
		this.delegate = delegate;
	}

	public void setDatasetID(String datasetID) {
		this.apiKey = datasetID;
	}
	
	public void setDeviceID(String deviceID) {
		if(deviceID != null){
			this.deviceID = deviceID;
		} else {
			this.deviceID = "DEVICE_ID_NOT_AVAILABLE";
		}
	}
	
	public String getLastRawRespose(){
		return lastRawRespose;
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	public void query(byte[] queryImage) {
		PixlinqRequest request = new PixlinqRequest();
		request.apiKey = this.apiKey;
		request.queryImage = queryImage;
		
		new PixlinqAsyncTask().execute(request);
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	private String parsePixlinqResponse(String response){
		JSONObject responseObject;
		String result = null;

		if(response != null) {
			try {
				responseObject = (JSONObject) new JSONTokener(response).nextValue();
				JSONArray matches = responseObject.getJSONArray("matches");
				JSONArray errors = responseObject.getJSONArray("errors");
				if(matches.length() > 0){
					JSONObject match = matches.getJSONObject(0);
					String title = match.getString("title");
					String message = match.getString("message");
					String sanatizedMessage = message.split("\\.")[0];
					
					result = sanatizedMessage;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			result = null;
		}
		return result;
	}
	
	private class PixlinqAsyncTask extends AsyncTask<PixlinqRequest, Void, String> {
		
		private final String postBoundary = "------------------------pixlinq";
		private boolean networkError = false;
		
		protected String doInBackground(PixlinqRequest... params) {
			PixlinqRequest requestData = params[0];
			return pixlinqHttpRequest(requestData);
		}
		
		protected void onPostExecute(String result){
			if(networkError){
				delegate.onNetworkError();
			} else {
				lastRawRespose = result;
				String resultID = parsePixlinqResponse(result);
				delegate.irRequestFinished(resultID);
			}
		}
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
		private String pixlinqHttpRequest(PixlinqRequest requestData){
			int timeout = 10000; 
			byte[] requestBody = getRequestBody(requestData.apiKey, requestData.queryImage);
			String response = null;
			
			URL url;
			try {
				url = new URL("http://api.pixlinq.com/v1/searchimage");
				// setup HttpRequest:
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(timeout);
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="+postBoundary);
				connection.setFixedLengthStreamingMode(requestBody.length);

				OutputStream requestBodyStream = new BufferedOutputStream(connection.getOutputStream());
				requestBodyStream.write(requestBody);
				requestBodyStream.close();
				
				// connect:
				connection.connect();
				int responseCode = connection.getResponseCode();
				
				// receive response:
				InputStream responseStream = connection.getInputStream();
				
				// convert InputStream to String:
				BufferedReader bReader = new BufferedReader(new InputStreamReader(responseStream));
				StringBuilder sBuilder = new StringBuilder();
				String line;
				while( (line = bReader.readLine()) != null){
					sBuilder.append(line);
				}
				bReader.close();
				
				response = sBuilder.toString();
				
				connection.disconnect();
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				networkError = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return response;
		}
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
		private byte[] getRequestBody(String apiKey, byte[] requestImage) {
			String deviceIDString = deviceID + "_ANDROID_" + Build.VERSION.CODENAME + "_" + Build.VERSION.RELEASE + "_SDK:" +Build.VERSION.SDK_INT;
			
			String dataSeperation = "\n--"+postBoundary+"\n";
			String requestBodyString = "";
			
			requestBodyString += dataSeperation;
			requestBodyString += "Content-Disposition: form-data; name=\"api_key\"\n\n";
			requestBodyString += apiKey;
			
			requestBodyString += dataSeperation;
			requestBodyString += "Content-Disposition: form-data; name=\"device_id\"\n\n";
			requestBodyString += deviceIDString;
			
			requestBodyString += dataSeperation;
			requestBodyString += "Content-Disposition: form-data; name=\"image_file\"; filename=\"pixlinq_image.jpg\"\n";
			requestBodyString += "Content-Type: image/jpeg\n\n";
			
			String requestBodyEnd = "\n--"+postBoundary+"--\n";
			
			byte[] bodyStart = getUTF8StringBytes(requestBodyString);
			byte[] bodyEnd = getUTF8StringBytes(requestBodyEnd);
			
			byte[] body = new byte[bodyStart.length + requestImage.length + bodyEnd.length];
			System.arraycopy(bodyStart, 0, body, 0, bodyStart.length);
			System.arraycopy(requestImage, 0, body,  bodyStart.length, requestImage.length);
			System.arraycopy(bodyEnd, 0, body,  bodyStart.length + requestImage.length , bodyEnd.length);
			
			return body;
		}
		
		private byte[] getUTF8StringBytes(String source){
			byte[] resultBytes = null;
			try {
				resultBytes = source.getBytes("UTF8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return resultBytes;
		}
		
	}

}
