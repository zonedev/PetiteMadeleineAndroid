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
 * Issue
 * 
 * Representation of a print publication.
 * Contains a collection of Entries (possibly in multiple languages).
 * Optionally an URL to a PDF of the publication can be provided.
 */

package at.zone.madeleine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Issue {
	
	public class IssueMetaData {
		public String name;
		public String issue_pdf;
	}
	
	public HashMap<String, IssueMetaData> metaData;				// keyed by language
	public HashMap<String, HashMap<String, Entry> > entries;	// keyed by language, resulting hashmap is keyed by IR/Plist key
	public ArrayList<String> supportedLanguages;
	private String dataset;
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// create Issue structure with data obtained through PListLoader:
	public void inflateFromHashMap(HashMap<String, Object> data){
		entries = new HashMap<String,  HashMap<String, Entry> >();
		metaData = new HashMap<String, IssueMetaData >();
		
		ArrayList<String> foundLanguages = new ArrayList<String>();
		for (Map.Entry<String, Object> entry : data.entrySet())	{
			String key = entry.getKey();
			
			if(key.equalsIgnoreCase("config")){
				HashMap<String, Object> dict = (HashMap<String, Object>) entry.getValue();
				this.dataset = (String) dict.get("dataset");
			} else {
				String language = key;
				foundLanguages.add(language);
				IssueMetaData localizedMetaData = new IssueMetaData();
				
				HashMap<String, Object> issueLocalizedData = (HashMap<String, Object>) entry.getValue();
				for (Map.Entry<String, Object> localizedIssueData : issueLocalizedData.entrySet()) {
					String liKey = localizedIssueData.getKey();
					// process metadata:
					if(liKey.equalsIgnoreCase("name")){
						localizedMetaData.name = (String) localizedIssueData.getValue();
					}
					if(liKey.equalsIgnoreCase("issue_pdf")){
						localizedMetaData.issue_pdf = (String) localizedIssueData.getValue();
					}
					// process entries:
					if(liKey.equalsIgnoreCase("entries")){
						HashMap<String, Entry> localizedEntries = new HashMap<String, Entry>();
						HashMap<String, Object> entriesData = (HashMap<String, Object>) localizedIssueData.getValue();
						for (Map.Entry<String, Object> entryData : entriesData.entrySet())
						{
							Entry newEntry = new Entry();
							newEntry.inflateFromHashMap((String)entryData.getKey(),  (HashMap<String, Object>) entryData.getValue() );
							localizedEntries.put(newEntry.key, newEntry);
						}
						entries.put(language, localizedEntries );
					}
				}
				metaData.put(language, localizedMetaData );
				
			}
			
		}
		supportedLanguages = foundLanguages;
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - A C C E S S O R S
	public Entry getEntry(String key, String language){
		Entry requestedEntry = null;
		HashMap<String, Entry> localizedEntries = entries.get(language);
		if(localizedEntries != null){
			requestedEntry = localizedEntries.get(key);
			if(requestedEntry == null){
				// requested entry not found
			}
		} else {
			// requested entry from unsupported language
		}
		return requestedEntry;
	}
	
	public String getDataset(){
		return dataset;
	}
	
}
