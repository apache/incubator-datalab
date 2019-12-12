package dlab;

import org.apache.dlab.util.PropertyHelper;

public interface Constants {
	String API_URI = PropertyHelper.read("dlab.api.base.uri");
	String LOCAL_ENDPOINT = "local";
}
