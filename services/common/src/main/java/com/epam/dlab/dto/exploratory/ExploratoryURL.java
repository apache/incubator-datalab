package com.epam.dlab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Describe URL of exploratory.
 */
public class ExploratoryURL {
	@JsonProperty("description")
	private String description;
	@JsonProperty("url")
	private String url;
	
	/** Returns the description. */
	public String getDescription() {
		return description;
	}
	
	/** Sets the description. */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/** Sets the description. */
	public ExploratoryURL withDescription(String description) {
		setDescription(description);
		return this;
	}

	/** Returns the URL. */
	public String getUrl() {
		return url;
	}

	/** Sets the URL. */
	public void setUrl(String url) {
		this.url = url;
	}

	/** Sets the URL. */
	public ExploratoryURL withUrl(String url) {
		setUrl(url);
		return this;
	}

    public int compareTo(ExploratoryURL obj) {
    	if (obj == null) {
    		return 1;
    	}

    	int result = 0;
    	if (this.description == null) {
    		if (obj.description != null) {
    			return -1;
    		}
    	} else {
    		result = this.description.compareTo(obj.description);
        	if (result != 0) {
        		return result;
        	}
    	}
    	
    	if (this.url == null) {
    		if (obj.url != null) {
    			return -1;
    		}
    	} else {
    		return this.url.compareTo(obj.url);
    	}
    	
    	return 0;
      }
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ExploratoryURL ?
				(this.compareTo((ExploratoryURL)obj) == 0) : false);

	}
}
