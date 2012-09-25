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
 *  Generalized interface for image recognition web services
 */

package at.zone.madeleine.ir;

public interface IRService {
	
	public void setDelegate(IRCallback delegate);
	public void setDatasetID(String datasetID);
	public void setDeviceID(String deviceID);
	public void query(byte[] queryImage);
	
	public String getLastRawRespose();
	
}
