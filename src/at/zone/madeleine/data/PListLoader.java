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
 * PListLoader.java
 * 
 * Asynchronously downloads and parses property list files.
 * see http://en.wikipedia.org/wiki/Property_list
 * or https://developer.apple.com/library/mac/#documentation/Cocoa/Conceptual/PropertyLists/Introduction/Introduction.html
 * for further details on property lists.
 * 
 * This is not a general parser for Plist data. Only the
 * specific structure used by petite madeleine as well as
 * the following data types are supported:
 * <array>, <dict>, <string>.
 */

package at.zone.madeleine.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.AsyncTask;

public class PListLoader {

	private String tag; // can be used to pass extra info about the downloaded
						// data in the callback for example an issueKey 
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// Initiate download and parsing of remote property list:
	public void loadPListFromUrl(String url, PListLoaderCallback delegate){
		new DomFromUrlAsyncTask().execute(url, delegate);
	}

	public void loadPListFromUrl(String url, PListLoaderCallback delegate, String tag){
		this.tag = tag;
		loadPListFromUrl(url, delegate);
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - P A R S I N G
	// translate Document object with PList data into a HashMap structure:
	private static HashMap<String, Object> hashMapFromDOM(Document dom){
		Element rootElement = dom.getDocumentElement(); // <plist> element
		Element mainElement = (Element) rootElement.getChildNodes().item(1); // root <dict> or <array> element
		
		if(isArrayElement(mainElement)){
			HashMap<String, Object> container = new HashMap<String, Object>();
			container.put("array", arrayElement2ArrayList(mainElement));
			return container;
		} else {
			return dictElement2HashMap(mainElement);
		}
	}
	// translate Element object with <dict> data into a HashMap structure:
	private static HashMap<String, Object> dictElement2HashMap(Element element){
		HashMap<String, Object> data = new HashMap<String, Object>();
		
		// extract all element nodes:
		NodeList children = element.getChildNodes();
		ArrayList<Element> childElements = new ArrayList<Element>();
			
		for(int i=0;i<children.getLength();i++){
			Node child = children.item(i);
			if(child instanceof Element){
				childElements.add((Element) child);
			}
		}
		// iterate over child elements:
		for(int i=0;i<childElements.size();i+=2){
			Element keyElement = (Element) childElements.get(i);
			String keyName = keyElement.getTextContent();
			Element contentElement = (Element) childElements.get(i+1);
			// simply add it to hash if its a string value:
			if(isStringElement(contentElement)){
				data.put(keyName, contentElement.getTextContent());
			}
			// recrusively add if its another dict structure:
			if(isDictionaryElement(contentElement)){
				HashMap<String, Object> childData = dictElement2HashMap(contentElement);
				data.put(keyName, childData);
			}
			if(isArrayElement(contentElement)){
				data.put(keyName, arrayElement2ArrayList(contentElement) );
			}
		}
			
		return data;
	}
	
	// translate Element object with <array> data into a ArrayList:
	private static ArrayList arrayElement2ArrayList(Element element) {
		ArrayList data = new ArrayList();
		
		// extract all element nodes:
		NodeList children = element.getChildNodes();
		ArrayList<Element> childElements = new ArrayList<Element>();
		
		for(int i=0;i<children.getLength();i++){
			Node child = children.item(i);
			if(child instanceof Element){
				childElements.add((Element) child);
			}
		}
		
		// iterate over child elements:
		for(int i=0;i<childElements.size();i++){
			Element contentElement = (Element) childElements.get(i);
			if(isStringElement(contentElement)){
				data.add(contentElement.getTextContent());
			}
			if(isArrayElement(contentElement)){
				data.add(arrayElement2ArrayList(contentElement));
			}
			if(isDictionaryElement(contentElement)){
				data.add(dictElement2HashMap(contentElement));
			}
		}
		
		return data;
	}	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - utility methods:
	private static boolean isDictionaryElement(Element element){
		return isElementType(element, "dict");
	}
	
	private static boolean isStringElement(Element element){
		return isElementType(element, "string");
	}
	
	private static boolean isArrayElement(Element element) {
		return isElementType(element, "array");
	}
	
	private static boolean isElementType(Element element, String name){
		String tagName = element.getTagName();
		return tagName.equalsIgnoreCase(name);
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// AsyncTask to download plist and parse its xml structure:
	private class DomFromUrlAsyncTask extends AsyncTask<Object, Void, Document> {
		
		private PListLoaderCallback delegate;
		private boolean networkError = false;
		
		protected Document doInBackground(Object... arg0) {
			String url = (String) arg0[0];
			this.delegate = (PListLoaderCallback) arg0[1];
			return getDomFromUrl(url);
		}
		
		protected void onPostExecute(Document result){
			if(networkError){
				delegate.onNetworkError();
			} else {
				delegate.plistDataReady(hashMapFromDOM(result), tag);
			}
		}
		
		private Document getDomFromUrl(String url){
			Document dom = null;
			
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				dom = db.parse(url);
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				networkError = true;
			}
			
			return dom;
		}
		
	}
}
