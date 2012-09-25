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
 * Entry
 * 
 * Representation of an image and its linked media.
 */
package at.zone.madeleine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Entry {
	
	public String key;
	public String description;
	public ArrayList<MediaAttachment> mediaAttachments;
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// create Entry structure with data obtained through PListLoader:
	public void inflateFromHashMap(String key, HashMap<String, Object> data){
		this.key = key;
		ArrayList<MediaAttachment> attachments = new ArrayList<MediaAttachment>();
		for (Map.Entry<String, Object> kvPair : data.entrySet()) {
			String dkey = kvPair.getKey();
			if(dkey.equalsIgnoreCase("description")){
				this.description = (String)kvPair.getValue();
			}
			if(MediaAttachment.isMediaAttachmentKey(dkey)){
				MediaAttachment newAttachment = new MediaAttachment();
				try {
					newAttachment.inflateFromHashMap( dkey, (HashMap<String, Object>) kvPair.getValue() );
					attachments.add(newAttachment);
				} catch (ClassCastException e){
					e.printStackTrace();
				}
				
			}
		}
		this.mediaAttachments = attachments;
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// convenience method to directly get the entry if there is only one:
	public MediaAttachment getSingleAttachment(){
		if(this.hasMultipleAttachments()){
			return null;
		} else {
			return mediaAttachments.get(0);
		}
	}
	
	public boolean hasMultipleAttachments(){
		return mediaAttachments.size() > 1;
	}
	
}
