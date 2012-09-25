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

import java.util.HashMap;

public class MediaAttachment {

	public static enum MediaType {
		VIDEO, PDF, BROWSER, SLIDESHOW, YOUTUBE;
	}
	
	private static final HashMap<String, MediaType> mediaTypeMap;
	static {
		mediaTypeMap = new HashMap<String, MediaType>();
		mediaTypeMap.put("media", MediaType.VIDEO);
		mediaTypeMap.put("text", MediaType.PDF);
		mediaTypeMap.put("website", MediaType.BROWSER);
		mediaTypeMap.put("safari", MediaType.BROWSER);
		mediaTypeMap.put("slideshow", MediaType.SLIDESHOW);
		mediaTypeMap.put("youtube", MediaType.YOUTUBE);
	}
	
	public MediaType type;
	public String description;
	public String url;
	
	public void inflateFromHashMap(String key, HashMap<String, Object> data){
		this.type = checkMediaType(key);
		this.description = (String) data.get("description");
		if(description == null){
			description = "no description found";
		}
		this.url = (String) data.get("url");
	}
	
	public static boolean isMediaAttachmentKey(String key){
		return checkMediaType(key) != null;
	}
	
	// used to vertify media type of files downloaded with FileDownloader
	public static MediaType getMediaTypeFromFilename(String filename){
		MediaType type = null;
		String parts[] = filename.split("\\.");
		String suffix = parts[parts.length-1];

		if(suffix.equalsIgnoreCase("pdf")){
			type = MediaType.PDF;
		}
		if(suffix.equalsIgnoreCase("mp4")){
			type = MediaType.VIDEO;
		}
		if(suffix.equalsIgnoreCase("m4v")){
			type = MediaType.VIDEO;
		}
		
		return type;
	}
	
	private static MediaType checkMediaType(String mediaKey){
		String sanKey = sanatizeMediaKey(mediaKey);
		return mediaTypeMap.get(sanKey);
	}
	
	private static String sanatizeMediaKey(String key){
		String [] tokens = key.split(" - ");
		String baseKey = tokens[0];
		return baseKey;
	}
	
}
