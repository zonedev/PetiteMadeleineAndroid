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

package at.zone.madeleine.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import android.os.AsyncTask;

public class FileDownloader {
	
	public void downloadFile(String url, FileDownloadCallback delegate){
		new FileDownloadAsyncTask().execute(url, delegate);
	}
	
	private class FileDownloadAsyncTask extends AsyncTask<Object, Void, File> {
		
		private FileDownloadCallback delegate;
		private boolean networkError = false;
		
		@Override
		protected File doInBackground(Object... arg0) {
			String url = (String) arg0[0];
			this.delegate = (FileDownloadCallback) arg0[1];
			return downloadFile(url);
		}
		
		@Override
		protected void onPostExecute(File file){
			if(networkError){
				delegate.onNetworkError();
			} else {
				delegate.downloadFinished(file);
			}
		}
		
		private File downloadFile(String urlString){
			File file = null;
			
			URL url;
			try {
				url = new URL(urlString);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);
				urlConnection.connect();
				
				File SDCardRoot = new File(ContentManager.getInstance().getApplicationFolderPath());
				String[] urlParts = urlString.split("/");
				String filename = urlParts[urlParts.length-1];
				
				if (!isFileAlreadyOnDisk(filename)) {
					file = new File(SDCardRoot, filename);
					//this will be used to write the downloaded data into the file we created
					FileOutputStream fileOutput = new FileOutputStream(file);
					//this will be used in reading the data from the internet
					InputStream inputStream = urlConnection.getInputStream();
					int totalSize = urlConnection.getContentLength();
					int downloadedSize = 0;
					
					// create a buffer
					byte[] buffer = new byte[1024];
					int bufferLength = 0; // used to store a temporary size of the buffer
					// now, read through the input buffer and write the contents to the file
					while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
							fileOutput.write(buffer, 0, bufferLength);
							downloadedSize += bufferLength;
					}
					fileOutput.close();
				
				} else {
					// file already there, just grab it
					file = new File(ContentManager.getInstance().getApplicationFolderPath().concat(filename));
				}
				
				urlConnection.disconnect();
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (ProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				networkError = true;
				file.delete(); // remove (partially complete) temp file!
			}
			
			return file;
		}
		
		private boolean isFileAlreadyOnDisk(String filename) {
			boolean fileExists = false;
			File appDir = new File(ContentManager.getInstance().getApplicationFolderPath());
			if (appDir != null && appDir.isDirectory()){
				File[] filenames = appDir.listFiles();
				for (File tmpf : filenames){ 
					if(tmpf.getName().equalsIgnoreCase(filename)){
						fileExists = true;
					}
				}
			}
			return fileExists;
		}
		
	}
	
}
