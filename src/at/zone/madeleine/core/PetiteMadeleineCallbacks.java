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

import at.zone.madeleine.data.Entry;
import at.zone.madeleine.data.NetworkTaskCallback;

public interface PetiteMadeleineCallbacks extends NetworkTaskCallback {
	
	public void contentManagerUpdateStarted();
	public void contentMangerUpdateFinished(Boolean validData);
	
	public void recognizedEntry(Entry entry);
	public void imageNotRecognized();
	public void noEntryForMatch(String matchID);
	
}
