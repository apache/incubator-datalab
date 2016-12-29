/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/


package com.epam.dlab.backendapi.core.response.folderlistener;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static com.epam.dlab.backendapi.core.Constants.JSON_EXTENSION;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.response.folderlistener.WatchItem.ItemStatus;
import com.epam.dlab.exceptions.DlabException;

/** Listen the directories for the files creation and runs the file processing by {@link AsyncFileHandler}.
 */
public class FolderListener implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(FolderListener.class);
	
	/** Timeout of the check the file creation in milliseconds. */
	public static final long LISTENER_TIMEOUT_MILLLIS = 1000;
	
	/** Timeout of the idle for the folder listener in milliseconds. */
	public static final long LISTENER_IDLE_TIMEOUT_MILLLIS = 5000;

	/** Timeout of waiting for the directory creation in milliseconds. */
	private static final long WAIT_DIR_TIMEOUT_MILLIS = 500; 

	/** List of the folder listeners. */
	private static final List<FolderListener> listeners = new ArrayList<FolderListener>();

	
	/** Appends the file handler for processing to the folder listener and returns instance of the file handler. 
	 * @param directoryName Name of directory for listen.
	 * @param fileHandlerCallback File handler for processing.
	 * @param timeoutMillis Timeout waiting for the file creation in milliseconds.
	 * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
	 * @return Instance of the file handler.
	 */
	public static WatchItem listen(String directoryName, FileHandlerCallback fileHandlerCallback,
			long timeoutMillis, long fileLengthCheckDelay) {
		return listen(directoryName, fileHandlerCallback, timeoutMillis, fileLengthCheckDelay, null);
	}

	/** Appends the file handler for processing to the folder listener for the existing file and returns
	 * instance of the file handler. If the file name is <b>null</b> this means that file does not exist
	 * and equal to call method {@link FolderListener#listen(String, FileHandlerCallback, long, long)}. 
	 * @param directoryName Name of directory for listen.
	 * @param fileHandlerCallback File handler for processing.
	 * @param timeoutMillis Timeout waiting for the file creation in milliseconds.
	 * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
	 * @param fileName file name.
	 * @return Instance of the file handler.
	 */
	public static WatchItem listen(String directoryName, FileHandlerCallback fileHandlerCallback,
			long timeoutMillis, long fileLengthCheckDelay, String fileName) {
		FolderListener listener;
		WatchItem item;
		
		LOGGER.debug("Looking for folder listener to folder \"{}\" ...", directoryName);
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				listener = listeners.get(i);
				if (listener.itemList.getDirectoryName().equals(directoryName)) {
					if (listener.isAlive()) {
						LOGGER.debug("Folder listener \"{}\" found. Append file handler for UUID {}",
								directoryName, fileHandlerCallback.getUUID());
						item = listener.itemList.append(fileHandlerCallback, timeoutMillis, fileLengthCheckDelay, fileName);
						return item;
					} else {
						LOGGER.warn("Folder listener \"{}\" is dead and will be removed", directoryName);
						listeners.remove(i);
						break;
					}
				}
			}
			LOGGER.debug("Folder listener \"{}\" not found. Create new listener and append file handler for UUID {}",
					directoryName, fileHandlerCallback.getUUID());
			listener = new FolderListener(directoryName);
			item = listener.itemList.append(fileHandlerCallback, timeoutMillis, fileLengthCheckDelay, fileName);
			listeners.add(listener);
			listener.start();
		}
		return item;
	}

	/** Terminates all the folder listeners. */
	public static void terminateAll() {
		FolderListener[] array;
		synchronized (listeners) {
			array = listeners.toArray(new FolderListener[listeners.size()]);
		}
		for (int i = 0; i < array.length; i++) {
			array[i].terminate();
		}
	}
	
	/** Returns the list of folder listeners. */
	public static List<FolderListener> getListeners() {
		return listeners;
	}
	

	/** Thread of the folder listener. */
	private Thread thread;
	/** List of the file handles. */
	private WatchItemList itemList;
	/** Watch service for the directory. */
	private WatchService watcher;
	/** Flag of listening status. */
	private boolean isListen = false;
	/** Time when expired of idle for folder listener in milliseconds. */
	private long expiredIdleMillis = 0;
		
	
	private FolderListener() {	}
	
	/** Creates thread of the folder listener
	 * @param directoryName Name of directory.
	 */
	private FolderListener(String directoryName) {
		itemList = new WatchItemList(directoryName);
	}

	/** Starts the thread of the folder listener. */
	protected void start() {
		thread = new Thread(this, getClass().getSimpleName() + "-" + listeners.size());
		thread.start();
	}
	
	/** Terminates the thread of the folder listener. */
	protected void terminate() {
		if (thread != null) {
			LOGGER.debug("Folder listener \"{}\" will be terminate", getDirectoryName());
			thread.interrupt();
		}
	}
	
	/** Returns <b>true</b> if the folder listener thread is running and is alive, otherwise <b>false</b>. */
	public boolean isAlive() {
		return (thread != null ? thread.isAlive() : false);
	}
	
	/** Returns <b>true</b> if the folder listener is listening the folder. */
	public boolean isListen() {
		return isListen;
	}
	
	
	/** Returns the list of the file handlers. */
	public WatchItemList getItemList() {
		return itemList;
	}
	
	/** Returns the full name of directory. */
	public String getDirectoryName() {
		return itemList.getDirectoryFullName();
	}
	
	/** Waiting for the directory creation and returns <b>true</b> if it exists or created.
	 * If timeout has expired and directory was not created returns <b>false</b> */
	private boolean waitForDirectory() throws InterruptedException {
    	File file = new File(getDirectoryName());
		if (file.exists()) {
    		return true;
    	} else {
    		LOGGER.debug("Folder listener \"{}\" waiting for the directory creation", getDirectoryName());
    	}

		long expiredTimeMillis = itemList.get(0).getExpiredTimeMillis();
		while (expiredTimeMillis >= System.currentTimeMillis()) {
    		Thread.sleep(WAIT_DIR_TIMEOUT_MILLIS);
    		if (file.exists()) {
        		LOGGER.debug("Folder listener \"{}\" - directory has been created", getDirectoryName());
        		return true;
        	}
    	}
		LOGGER.error("Folder listener \"{}\" error. Timeout has expired and directory does not exist", getDirectoryName());
    	return false;
    }

	/** Initializes the thread of the folder listener. Returns <b>true</b> if the initialization
	 * completed successfully. Returns <b>false</b> if all the file handlers has been processed
	 * or initialization fails. */
	private boolean init() {
		LOGGER.debug("Folder listener initializing for \"{}\" ...", getDirectoryName());
    	
		try {
    		if (!waitForDirectory()) {
    			return false;
    		}
    	} catch (InterruptedException e) {
    		LOGGER.debug("Folder listener \"{}\" has been interrupted", getDirectoryName());
    		return false;
    	}

		processStatusItems();
		if (itemList.size() == 0) {
			LOGGER.debug("Folder listener \"{}\" have no files and will be finished", getDirectoryName());
			return false;
		}
		
		watcher = null;
		try {
			watcher = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(getDirectoryName());
			path.register(watcher, ENTRY_CREATE);
		} catch (IOException e) {
			if (watcher != null) {
				try {
					watcher.close();
				} catch (IOException e1) {
					LOGGER.debug("Folder listener for \"{}\" closed with error.", getDirectoryName(), e1);
				}
				watcher = null;
			}
			throw new DlabException("Can't create folder listener for \"" + getDirectoryName() + "\".", e);
		}

		LOGGER.debug("Folder listener has been initialized for \"{}\" ...", getDirectoryName());
		return true;
	}
	
	/** Process all the file handlers if need and removes all expired, processed or interrupted
	 * the file handlers from the list of the file handlers. */
	private void processStatusItems() {
		int i = 0;
		
		if (itemList.size() > 0 ) {
			expiredIdleMillis = 0;
		}
		itemList.processItemAll();
		
		synchronized (itemList) {
			while (i < itemList.size()) {
				final WatchItem item = itemList.get(i);
				final ItemStatus status = item.getStatus();
				final String uuid = item.getFileHandlerCallback().getUUID();
				
				switch (status) {
				case WAIT_FOR_FILE:
				case FILE_CAPTURED:
				case INPROGRESS:
					// Skip
					i++;
					continue;
				case TIMEOUT_EXPIRED:
					LOGGER.warn("Folder listener \"{}\" remove expired file handler for UUID {}", getDirectoryName(), uuid);
					break;
				case IS_DONE:
					if ( item.getFutureResult() ) {
						LOGGER.debug("Folder listener \"{}\" remove processed file handler for UUID {}, handler result is {}", getDirectoryName(), uuid, item.getFutureResult());
					} else {
						LOGGER.warn("Folder listener \"{}\" remove processed file handler for UUID {}, handler result is {}", getDirectoryName(), uuid, item.getFutureResult());
					}
					break;
				case IS_CANCELED:
					LOGGER.debug("Folder listener \"{}\" remove canceled file handler for UUID {}", getDirectoryName(), uuid);
					break;
				case IS_FAILED:
					LOGGER.warn("Folder listener \"{}\" remove failed file handler for UUID {}", getDirectoryName(), uuid);
					break;
				case IS_INTERRUPTED:
					LOGGER.debug("Folder listener \"{}\" remove iterrupted file handler for UUID {}", getDirectoryName(), uuid);
					break;
				default:
					continue;
				}
				itemList.remove(i);
			}
		}
		
		if (expiredIdleMillis == 0 && itemList.size() == 0) {
			expiredIdleMillis = System.currentTimeMillis() + LISTENER_IDLE_TIMEOUT_MILLLIS;
		}
	}
	
	/** Removes the listener from the list of folder listeners if the the file handler list is empty
	 * and idle time has expired or if <b>force</b> flag has been set to <b>true</b>.
	 * @param force the flag of remove the folder listener immediately.
	 * @return <b>true</b> if the folder listener has been removed otherwise <b>false</>.
	 */
	private boolean removeListener(boolean force) {
		synchronized (listeners) {
			if (force || (expiredIdleMillis != 0 && expiredIdleMillis < System.currentTimeMillis())) {
				for (int i = 0; i < listeners.size(); i++) {
					if (listeners.get(i) == this) {
						isListen = false;
						listeners.remove(i);
						LOGGER.debug("Folder listener \"{}\" has been removed from pool", getDirectoryName());
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/** Waiting for files and process it. */
	private void pollFile() {
		try {
			isListen = true;
			while (true) {
				WatchKey key;
				LOGGER.trace("Folder listener \"{}\" poll calling ...", getDirectoryName());
				key = watcher.poll(LISTENER_TIMEOUT_MILLLIS, TimeUnit.MILLISECONDS);
				LOGGER.trace("Folder listener \"{}\" poll called", getDirectoryName());
	
				if (key != null) {
					for ( WatchEvent<?> event : key.pollEvents() ) {
						WatchEvent.Kind<?> kind = event.kind();
						if (kind == ENTRY_CREATE) {
							String fileName = event.context().toString();
							LOGGER.trace("Folder listener \"{}\" checks the file {}", getDirectoryName(), fileName);

							try {
								if (fileName.endsWith(JSON_EXTENSION)) {
									WatchItem item = itemList.getItem(fileName);
									if (item != null && item.getFileName() == null) {
										LOGGER.debug("Folder listener \"{}\" handles the file {}", getDirectoryName(), fileName);
										item.setFileName(fileName);
										if (itemList.processItem(item)) {
											LOGGER.debug("Folder listener \"{}\" processes the file {}", getDirectoryName(), fileName);
										}
									}
								}
							} catch (Exception e) {
								LOGGER.warn("Folder listener \"{}\" has got exception for check or process the file {}", getDirectoryName(), fileName, e);
							}
						}
					}
					key.reset();
				}
				
				processStatusItems();
				if (removeListener(false)) {
					LOGGER.debug("Folder listener \"{}\" have no files and will be finished", getDirectoryName());
					break;
				}
			}
		} catch (InterruptedException e) {
			removeListener(true);
			LOGGER.debug("Folder listener \"{}\" has been interrupted", getDirectoryName());
		} catch (Exception e) {
			removeListener(true);
			LOGGER.error("Folder listener for \"{}\" closed with error.", getDirectoryName(), e);
			throw new DlabException("Folder listener for \"" + getDirectoryName() + "\" closed with error. " + e.getLocalizedMessage(), e);
		} finally {
			try {
				watcher.close();
			} catch (IOException e) {
				LOGGER.debug("Folder listener for \"{}\" closed with error. {} ", getDirectoryName(), e.getLocalizedMessage());
			}
		}
	}

	@Override
	public void run() {
    	if (init()) {
			pollFile();
		} else {
			LOGGER.warn("Folder listener has not been initialized for \"{}\"", getDirectoryName());
			removeListener(true);
		}
	}
	
}