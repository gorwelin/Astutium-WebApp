package uk.ac.bangor.cs.ice2002.groupa2.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Controller
public class AstutiumAPIController {

	List<Map<String, Object>> basket = new LinkedList<Map<String,Object>>();
	
	@RequestMapping(value = "/domain", method = RequestMethod.GET)
	public String landingPageGET(Model uiModel) throws JsonParseException, JsonMappingException, IOException {
		File file = ResourceUtils.getFile("classpath:tld.json");
		
		ObjectMapper mapper = new ObjectMapper();
		
		String[] tlds = mapper.readValue(file, String[].class);
		
		uiModel.addAttribute("tlds", tlds);
		
			
		return "nounform";
	}
	
	
	@RequestMapping(value = "/domain/search/tld")
	@ResponseBody
	public List<TLDObject> getTLDs() throws JsonParseException, JsonMappingException, IOException {
		
		File file = ResourceUtils.getFile("classpath:tld2.json");
		
		ObjectMapper mapper = new ObjectMapper();
		List<TLDObject> tldList = mapper.readValue(file, new TypeReference<List<TLDObject>>(){});
		
		return tldList;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/domain/search/request")
	@ResponseBody
	public Map<String,Object> getDomainResponse(@RequestParam("domainrequest") String domain, @RequestParam("tld") String tld) throws IOException{
		

		
		
		URL url = new URL("http://arp.astutium.com/whoisrs/whois/domain/" + domain + tld);
		List<TLDObject> TLDFromParam = getTLDs().stream()
			     .filter(item -> item.getTld().equals(tld))
			     .collect(Collectors.toList());
		
		
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			StringBuilder builder = new StringBuilder();
			
			int statcode = 0;
			Map<String, Object> json;
			
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setReadTimeout(5000);
			
			
			statcode = connection.getResponseCode();
			
			builder.append("HTTP ");
			builder.append(statcode + " ");
			builder.append(connection.getResponseMessage());
			
			System.out.println(builder.toString());
			System.out.println("http://arp.astutium.com/whoisrs/whois/domain/" + domain + tld);
			
			if (statcode >= 200 && statcode < 300) {
				InputStream stream = connection.getInputStream();
				json = new ObjectMapper().readValue(stream, Map.class);
				
				Predicate<Object> filter = key -> key.equals("domain") || key.equals("available");
				
				Map<String, Object> newjson = json.entrySet().stream()
												.filter(entry -> filter.test(entry.getKey()))
												 .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
				newjson.put("domain", domain + tld);
				newjson.put("price", TLDFromParam.get(0).getPrice());
				
				return newjson;
				

				
			} else if(statcode == 503) {
				json = new HashMap<String, Object>();
				
				json.put("domain", domain+tld);
				json.put("available", false);
				json.put("price", TLDFromParam.get(0).getPrice());
			} else {
				json = new HashMap<String, Object>();
				
				json.put("domain", domain+tld);
				json.put("available", true);
				json.put("price", TLDFromParam.get(0).getPrice());
			}
			
			return json;
		} catch (SocketTimeoutException e) {
			Map<String, Object> json = new HashMap<String, Object>();
			
			
			json.put("domain", domain+tld);
			json.put("available", false);
			json.put("price", TLDFromParam.get(0).getPrice());
			return json;
		}
	
		
		
	}
	

	
	@RequestMapping(value = "/domain/search/{domain}/{tldindex}")
	public @ResponseBody Map<String, Object> getDomainByTLDIndex(@PathVariable("domain") String domain, @PathVariable("tldindex") int index) throws JsonParseException, JsonMappingException, IOException {
		List<TLDObject> tlds = getTLDs();
		
		return getDomainResponse(domain, tlds.get(tldIndex).getTld());
	}
	
	public int tldIndex = 0;
	
	@RequestMapping(value = "/domain/search", method = RequestMethod.POST)
	public String setviewtest(Model uiModel, @RequestParam(value="domaininput") String domain, @RequestParam("tld") String tld) throws IOException {		
		return "redirect:/domain/search/results/" + domain + "/" + tld;
	}
	
	@RequestMapping(value = "/domain/search/results/{domaininput}/{tld}", method = RequestMethod.GET)
	public String viewResults(Model uiModel, @PathVariable("domaininput") String domain, @PathVariable("tld") String tld) throws IOException {
		tldIndex = 0;
		List<TLDObject> tlds = getTLDs();
		
		//Map<String, Object> result = getDomainResponse(domain, tlds.get(tldIndex).getTld());
		Map<String, Object> result = getDomainResponse(domain, tld);
		Map<String, Object> itemToAdd = new HashMap<String, Object>();
		
		
		uiModel.addAttribute("result", result);
		uiModel.addAttribute("tldList", tlds);
		uiModel.addAttribute("domaininput", domain);
		uiModel.addAttribute("tld", tld);
		uiModel.addAttribute("itemtoadd", itemToAdd);
		
		return "testview";
	}
	
	@RequestMapping(value = "/domain/search/set", method = RequestMethod.GET)
	public @ResponseBody List<Map<String, Object>> getDomainSet(HttpServletRequest request) throws JsonParseException, JsonMappingException, IOException {
		int amountToLoad = 10;
		String domain = request.getParameter("domaininput");
		String tld = request.getParameter("tld");
		List<Map<String, Object>> listSet = new ArrayList<>();
		List<TLDObject> tlds = getTLDs();
		
		
		for (int i = 0; i < amountToLoad; i++) {
			TLDObject tldFromIndex = tlds.get(tldIndex);
			listSet.add(getDomainResponse(domain, tldFromIndex.getTld().toString()));
			
			
			
			if(tldIndex == tlds.size()) {
				return listSet;
			} else {
				tldIndex++;
			}
			
		}
		
		return listSet;
	}
	
	@RequestMapping(value = "/domain/search/set/popular", method = RequestMethod.GET)
	public @ResponseBody List<Map<String, Object>> getMostPopularSet(@RequestParam(value="domaininput") String domain) throws IOException {
		int[] popularTLDs = {0,4,3,6,5,8};
		List<Map<String, Object>> listSet = new ArrayList<>();
		List<TLDObject> tlds = getTLDs();
		
		
		for (int i = 0; i < popularTLDs.length; i++) {
			TLDObject tldFromIndex = tlds.get(popularTLDs[i]);
			listSet.add(getDomainResponse(domain, tldFromIndex.getTld()));
			
			
		}
		
		return listSet;
	
	}
	
	@RequestMapping(value = "/basket/get")
	public @ResponseBody List<Map<String, Object>> getBasket() {
		return basket;
	}
	
	@RequestMapping(value = "/addToBasket", method = RequestMethod.POST)
	public @ResponseBody boolean addToCart(@RequestParam("itemname") String itemName, @RequestParam("price") double price) {
		Map<String, Object> itemToAdd = new HashMap<>();
		itemToAdd.put("itemName", itemName);
		itemToAdd.put("price", price);
		basket.add(itemToAdd);
		
		return true;
	}
	
	@RequestMapping("/listset")
	public @ResponseBody List<Map<String, Object>> listByFilterSet(@RequestParam("domainname") String domain ,@RequestParam("tldChecked") List<Integer> tldIndexs) throws JsonParseException, JsonMappingException, IOException {
		List<TLDObject> tlds = getTLDs();
		List<Map<String, Object>> listSet = new ArrayList<>();
		for (int i = 0; i < tldIndexs.size(); i++) {
			TLDObject tldFromIndex = tlds.get(tldIndexs.get(i));
			listSet.add(getDomainResponse(domain, tldFromIndex.getTld()));
			
			
		}
		
		return listSet;
	}
	
	@RequestMapping("/basket/size")
	public @ResponseBody int basketSize() {
		return basket.size();
	}
	
	
	@RequestMapping("/domain/basket")
	public String getBasketView(Model uiModel) {
		uiModel.addAttribute("basketObj", basket);
		
		return "basket";
	}
	
	
}