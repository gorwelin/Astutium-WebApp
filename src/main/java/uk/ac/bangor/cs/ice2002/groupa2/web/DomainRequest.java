package uk.ac.bangor.cs.ice2002.groupa2.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DomainRequest {
	
	String domainName;
	boolean available;

	public DomainRequest(String domainName) throws IOException {
		Map<String, Object> domainSearch = getDomainResponse(domainName);
		
		domainName = (String) domainSearch.get("domain");
		available = (boolean) domainSearch.get("available");
	}

	
	@SuppressWarnings("unchecked")
	public Map<String,Object> getDomainResponse(String domainName) throws IOException{

		
		URL url = new URL("http://arp.astutium.com/whoisrs/whois/domain/" + domainName);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		StringBuilder builder = new StringBuilder();
		
		int statcode = 0;
		Map<String, Object> json;
		
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");
		
		statcode = connection.getResponseCode();
		
		builder.append("HTTP ");
		builder.append(statcode + " ");
		builder.append(connection.getResponseMessage());
		
		System.out.println(builder.toString());
		System.out.println("http://arp.astutium.com/whoisrs/whois/domain/" + domainName);
		
		if (statcode >= 200 && statcode < 300) {
			InputStream stream = connection.getInputStream();
			json = new ObjectMapper().readValue(stream, Map.class);
			
			Predicate<Object> filter = key -> key.equals("domain") || key.equals("available");
			
			Map<String, Object> newjson = json.entrySet().stream()
											.filter(filter)
											 .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
			
			return newjson;
			

			
		} else {
			json = new HashMap<String, Object>();
			
			json.put("domain", domainName);
			json.put("available", false);
		}
		
		return json;
	
		
		
	}
}
