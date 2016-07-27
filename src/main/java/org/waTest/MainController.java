package org.waTest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MainController {
	
	private Logger logger = LoggerFactory.getLogger(MainController.class);
	
	private String FIELD_ID = "Id";
	private String FIELD_DECISION = "Decision";
	

    @RequestMapping("/")
    public String main(Model model) {
        return "index";
    }

	
    @RequestMapping("/fileUpload")
    public String greeting(@RequestParam("inputFile") MultipartFile mFile, Model model) {
    	
    	String page = null;
    	
    	try (Reader reader = new InputStreamReader(mFile.getInputStream())) {
    		
    		CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(reader);
    		
    		List<CSVRecord> lines = parser.getRecords();
    		
    		List<String> notEvaluate = Arrays.asList(FIELD_ID, FIELD_DECISION);
    		
    		Map<String, Limits> mapLimits = this.processLimits(lines, parser.getHeaderMap(), notEvaluate);

    		
    		List<List<String>> table = new ArrayList<List<String>>();
    		
			List<String> headers = new ArrayList<String>(parser.getHeaderMap().size());
    		for (Entry<String, Integer> entry : parser.getHeaderMap().entrySet()) {
    			headers.add(entry.getValue(), entry.getKey());
    		}
			table.add(headers);
    		
    		for (CSVRecord record : lines) {
    			
    			if (recordIsToBeAdded(record, headers, notEvaluate, mapLimits)) {
    				
    				List<String> row = new ArrayList<String>(parser.getHeaderMap().size());
    	    		for (int i = 0; i < record.size(); i++) {
    	    			row.add(record.get(i));
    	    		}
    	    		table.add(row);
    			}
    		}
    		
    		logger.info(String.format("Generated a table with [%d] rows", table.size()));
    		
    		model.addAttribute("table", table);
    		
    		page = "table";
        	
    	} catch (IOException e) {
    		logger.error(e.getMessage(), e);
    		
            model.addAttribute("message", String.format("Input file [%s] seens not to be a valid CSV file!", mFile.getOriginalFilename()));
            
            page = "errorMessage";
		}
    	
        return page;
    }
    
    private boolean recordIsToBeAdded (CSVRecord record, List<String> headers, List<String> notEvaluate, Map<String, Limits> mapLimits) {
		
		boolean add = true;
		
		if (record.get(FIELD_DECISION).equals("0")) {
			for (String header: headers) {
				if (!notEvaluate.contains(header)) {
					Limits limits = mapLimits.get(header);
					int it = Integer.parseInt(record.get(header)); 
					
					if (!limits.between(it)) {
						add = false;
						break;
					}
				}
			}
		}
		return add;
    }
    
    private Map<String, Limits> processLimits(List<CSVRecord> lines, Map<String, Integer> headerMap, List<String> notEvaluate) {
    	
		Map<String, Limits> mapLimits = new HashMap<String, Limits>();
		
		for (CSVRecord record : lines) {
			
			if (record.get(FIELD_DECISION).equals("1")) {
    			for (Entry<String, Integer> entry : headerMap.entrySet()) {
    				
    				if (!notEvaluate.contains(entry.getKey())) {
    					Limits limits = mapLimits.get(entry.getKey());
    					if (limits == null) {
    						limits = new Limits();
    						mapLimits.put(entry.getKey(), limits);
    						
    						limits.max = limits.min = Integer.parseInt(record.get(entry.getValue()));
    					}
    					else {
    						int it = Integer.parseInt(record.get(entry.getValue()));
    						
    						limits.min = Math.min(it, limits.min);
    						limits.max = Math.max(it, limits.max);
    					}
    				}
    				
    			}
			}
		}
		
		logger.info("Limits: " + mapLimits.toString());
		
		return mapLimits;
    }
    
    
    public class Limits {
    	public int min;
    	public int max;
    	
    	public boolean between(int test) {
    		return (min <= test) && (test <= max);
    	}

		@Override
		public String toString() {
			return "Limits [min=" + min + ", max=" + max + "]";
		}
    	
    }

}
