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
 * ContentManager
 * 
 * ContentManager takes care of retrieving and managing all metadata.
 */

package at.zone.madeleine.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.os.Environment;

public class ContentManager implements PListLoaderCallback {
	
	private String masterListUrl   = "";
	private String contentLangauge = "de";
	
	private enum ContentManagerState {
		DOWNLOADING_MASTERLIST, PROCESSING_MASTERLIST, DOWNLOADING_ISSUES, INVALID_DATA, READY;
	}
	
	private final int DATA_EXPIRATION_MINUTES = 60;
	
	private static ContentManager instance;
	
	private HashMap<String, Issue> issues;
	private String defaultIssueKey;
	private Issue defaultIssue;
	private Issue activeIssue;
	
	private ContentManagerState state;
	private int issueDataToDownload;
	private ArrayList<ContentManagerCallback> delegates;
	
	private String mdlRootPath = Environment.getExternalStorageDirectory().toString() + "/petite_madeleine/";
	private String captureImagePath = mdlRootPath + "madeleineCapture.jpg";
	private String slideshowPath = mdlRootPath + "slideshow/";
	private String pdfPath = mdlRootPath + "pdfs/";
	
	private Date lastUpdate;
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  I N I T A L Z A T I O N
	public static synchronized ContentManager getInstance() {
		if(instance == null){ // create a new instance if needed
			instance = new ContentManager();
		}
		return instance;
	}
	// private constructor:
	private ContentManager() {
		this.delegates = new ArrayList<ContentManagerCallback>();
		createApplicationFolder();
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  
	// get new metadata from network:
	public void updateData(){
		if(!updateInProgress()){
			// cleanup:
			this.issues = new HashMap<String, Issue>();
			// download masterlist containing all issues and 
			// additional configuration data (e.g.: default_issue):
			this.state = ContentManagerState.DOWNLOADING_MASTERLIST;
			PListLoader plLoader = new PListLoader();
			plLoader.loadPListFromUrl(masterListUrl, this);
		}
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  C A L L B A C K S
	public void plistDataReady(HashMap<String, Object> data, String tag) {
		switch(this.state){
			case DOWNLOADING_MASTERLIST:
				processMasterList(data);
				break;
			case DOWNLOADING_ISSUES:
				addIssue(data, tag);
				break;
		}
	}
	
	public void onNetworkError() {
		this.state = ContentManagerState.INVALID_DATA;
		notifyDelegatesOfNetworkError();
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - D E S E R I A L I Z A T I O N
	// Processes master list and download every listed issue:
	private void processMasterList(HashMap<String, Object> masterPListData){
		state = ContentManagerState.PROCESSING_MASTERLIST;
		try {
			HashMap<String, Object> configData = (HashMap<String, Object>) masterPListData.get("config");
			HashMap<String, Object> issuesData = (HashMap<String, Object>) configData.get("issues");
			issueDataToDownload = issuesData.size();
			defaultIssueKey = (String) configData.get("default_issue");
			
			for (Map.Entry<String, Object> issueMetaData : issuesData.entrySet()) {
				String issueKey = issueMetaData.getKey();
				HashMap<String, Object> issueData = (HashMap<String, Object>) issueMetaData.getValue();
				String issueUrl = (String) issueData.get("data_url");
				
				state = ContentManagerState.DOWNLOADING_ISSUES;
				PListLoader plLoader = new PListLoader();
				plLoader.loadPListFromUrl(issueUrl, this, issueKey);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			state = ContentManagerState.INVALID_DATA;
			notifyDelegatesOfSuccess();
		}
	}
	// Create issue from downloaded data & check if downloads and proccesing is done:
	private void addIssue(HashMap<String, Object> issueData, String issueKey){
		Issue newIssue = new Issue();
		newIssue.inflateFromHashMap(issueData);
		this.issues.put(issueKey, newIssue);
		issueDataToDownload--;
		
		if(issueDataToDownload == 0){
			this.defaultIssue = this.issues.get(defaultIssueKey);
			this.activeIssue = this.defaultIssue;
			state = ContentManagerState.READY;
			// delete all old temporary files:
			removeSlideshowData();
			removeTempData();
			// ContentManager is ready - notify its delegates:
			notifyDelegatesOfSuccess();
		}
		// this should not happen - it's here only to detect possible bugs:
		if(issueDataToDownload < 0){
			state = ContentManagerState.INVALID_DATA;
			notifyDelegatesOfSuccess();
		}
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  U T I L I T Y  M E T H O D S
	private boolean updateInProgress(){
		return state == ContentManagerState.DOWNLOADING_MASTERLIST || state == ContentManagerState.DOWNLOADING_ISSUES || state == ContentManagerState.PROCESSING_MASTERLIST;
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  S T O R A G E
	public void createApplicationFolder() {
		// create folder if not existing already
		File mdlRootDir = new File(mdlRootPath);
		if(!mdlRootDir.isDirectory()) {
			mdlRootDir.mkdirs();
		}
		File slideshowDir = new File(slideshowPath);
		if(!slideshowDir.isDirectory()) {
			slideshowDir.mkdirs();
		}
	}
	
	public void removeSlideshowData() {
		String slideshowPath = this.slideshowPath;
		File slideshowDir = new File(slideshowPath);
		if (slideshowDir != null){
			File[] filenames = slideshowDir.listFiles();
			for (File tmpf : filenames){ 
				tmpf.delete();
			}
		}
	}
	
	public void removeCapturedImage() {
		File capturedImage = new File(getCaptureImagePath());
		if (capturedImage != null){
			capturedImage.delete();
		}
	}
	
	public void removeTempData() {
		String path = this.mdlRootPath;
		File appDir = new File(path);
		if (appDir != null){
			File[] filenames = appDir.listFiles();
			for (File tmpf : filenames) {
				tmpf.delete();
			}
		}
		// all folders gone, create them again:
		createApplicationFolder();
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  M A N A G E  D E L E G A T E S
	public void registerDelegate(ContentManagerCallback delegate){
		delegates.add(delegate);
	}

	public void unregisterDelegate(ContentManagerCallback delegate){
		delegates.remove(delegate);
	}
	
	private void notifyDelegatesOfSuccess() {
		lastUpdate = new Date();
		for(ContentManagerCallback delegate : delegates){
			delegate.contentManagerFinished(this);
		}
	}
	
	private void notifyDelegatesOfNetworkError() {
		lastUpdate = new Date();
		for(ContentManagerCallback delegate : delegates) {
			delegate.onNetworkError();
		}
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  A C C E S S O R S
	public void setMasterListURL(String url) {
		masterListUrl = url;
	}
	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . C o n t e n t  A c c e s s o r s
	public String getActiveContentLanguage() {
		return contentLangauge;
	}
	
	public void setActiveContentLanguage(String lang) {
		contentLangauge = lang;
	}
	
	public Issue getActiveIssue() {
		return activeIssue;
	}

	public String getLocalizedActiveIssueName() {
		return activeIssue.metaData.get(contentLangauge).name;
	}

	public String getLocalizedActiveIssuePdfUrl() {
		String pdfUrl = null;
		if(activeIssue != null) {
			pdfUrl = activeIssue.metaData.get(contentLangauge).issue_pdf;
		}
		return pdfUrl;
	}
	
	public void setActiveIssue(Issue issue) {
		activeIssue = issue;
	}
	
	public ArrayList<Issue> getAvailableIssues() {
		return new ArrayList<Issue>(issues.values());
	}
	
	public Entry getEntryForActiveIssue(String entryKey) {
		Entry entry = activeIssue.entries.get(contentLangauge).get(entryKey);
		return entry;
	}
	
	public String getActiveDataset() {
		return activeIssue.getDataset();
	}
	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .  C o n t e n t M a n a g e r  S t a t e
	public boolean hasValidData() {
		return state == ContentManagerState.READY;
	}
	
	public boolean hasCurrentData() {
		if(lastUpdate != null){
			long diffMillis = new Date().getTime() - lastUpdate.getTime();
			long thresholdMillis = DATA_EXPIRATION_MINUTES * 60 * 1000;
			if(diffMillis < thresholdMillis){
				return true;
			}
		}
		return false;
	}
	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 
	public String getApplicationFolderPath() {
		return mdlRootPath;
	}
	
	public String getSlideshowPath() {
		return slideshowPath;
	}
	
	public String getCaptureImagePath() {
		return captureImagePath;
	}
	
	public String getPdfPath() {
		return pdfPath;
	}
	
}
