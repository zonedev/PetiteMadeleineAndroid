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

package at.zone.madeleine.core;

import android.content.Context;
import android.telephony.TelephonyManager;
import at.zone.madeleine.data.ContentManager;
import at.zone.madeleine.data.ContentManagerCallback;
import at.zone.madeleine.data.Entry;
import at.zone.madeleine.ir.IRCallback;
import at.zone.madeleine.ir.IRService;

public class PetiteMadeleineCore implements ContentManagerCallback, IRCallback {

	private PetiteMadeleineCallbacks delegate;
	private Context context;
	
	public ContentManager contentManager;
	private IRService irService;
	
	private Entry currentEntry = null;
	private boolean readyForNextRequest = false;
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - I N I T
	public PetiteMadeleineCore(Context context, PetiteMadeleineCallbacks delegate, IRService anIRService, String masterListURL){
		this.delegate = delegate;
		this.context = context;
		
		contentManager = ContentManager.getInstance();
		contentManager.setMasterListURL(masterListURL);
		contentManager.registerDelegate(this);
		contentManager.updateData();
		
		irService = anIRService;
		irService.setDelegate(this);
	}

	private void setupIRService(){
		irService.setDatasetID(contentManager.getActiveDataset());
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		irService.setDeviceID(telephonyManager.getDeviceId());
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - L I F E  C Y C L E
	//
	// These methods *have* to be called by the Activity using PetiteMadeleineCore
	// during the corresponding life cycle events to ensure system integrity
	//
	public void onResume(){
		if(Utils.hasNetworkConnection(context)) {
			// make sure necessary data is available after returning to the activity:
			if(contentManager == null){
				contentManager = ContentManager.getInstance();
				contentManager.registerDelegate(this);
				contentManager.updateData();
				delegate.contentManagerUpdateStarted();
			} else {
				if(contentManager.hasCurrentData()){
					// reset irService if contentManger data is present
					// otherwise CM is currently loading and will set
					// irService when its done:
					if(contentManager.hasValidData()){
						setupIRService();
						readyForNextRequest = true;
					}
				} else {
					// update if contentManager is out of date or
					// there has been a previous network error:
					contentManager.updateData();
					readyForNextRequest = false;
					delegate.contentManagerUpdateStarted();
				}
			}
			contentManager.createApplicationFolder(); // recreate temp folder in case it has been deleted by user
		} else {
			onNetworkError();
		}
	}
	
	public void onDestroy(){
		// delete all temp files
		if(contentManager != null){
			// delete temp files in the slideshow folder
			contentManager.removeSlideshowData();
			// delete all other files in the main folder
			contentManager.removeTempData();
		}
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	public void sendIRRequest(byte[] queryImage) {
		irService.setDatasetID(contentManager.getActiveDataset());
		irService.query(queryImage);
	}	
	
	public void lock(){
		readyForNextRequest = false;
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	public boolean contentManagerIsReady(){
		return contentManager != null &&  contentManager.hasCurrentData() && contentManager.hasValidData();
	}
	
	public Boolean isReadyForNextRequest(){
		return readyForNextRequest;
	}
	
	public Entry getCurrentEntry(){
		return currentEntry;
	}
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - C A L L B A C K S
	public void contentManagerFinished(ContentManager contentManager) {
		if (contentManager.hasValidData()) {
			setupIRService();
		}
		readyForNextRequest = contentManager.hasValidData();
		delegate.contentMangerUpdateFinished(contentManager.hasValidData());
	}

	public void irRequestFinished(String result) {
		if (result != null) {
			Entry foundEntry = contentManager.getEntryForActiveIssue(result);
			if (foundEntry != null) {
				currentEntry = foundEntry;
				delegate.recognizedEntry(foundEntry);
			} else {
				delegate.noEntryForMatch(result);
			}
		} else {
			delegate.imageNotRecognized();
		}
		readyForNextRequest = true;
		// delete the query image
		contentManager.removeCapturedImage();
	}
	
	public void onNetworkError() {
		delegate.onNetworkError();
	}

}
