package com.smilecoms.rra.ucc.controller;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import com.smilecoms.rra.model.CDRData;
import com.smilecoms.rra.model.CDRDataRequest;
import com.smilecoms.rra.model.UCCUserData;
import com.smilecoms.rra.service.UCCDataService;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 
 * @author rajeshkumar
 *
 */
@RestController
@RequestMapping("/api/ucc")
public class UCCURestController {
	private static final Logger LOGGER = LoggerFactory.getLogger(UCCURestController.class);

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@Autowired
	private UCCDataService dataService;

	@RequestMapping(value = "/customers-csv", method = RequestMethod.GET)
        public void getUsersData(HttpServletResponse response) throws Exception {
            String filename = "smile-customers.csv";

            response.setContentType("text/csv");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");
        
            //create a csv writer
            StatefulBeanToCsv<UCCUserData> writer = new StatefulBeanToCsvBuilder<UCCUserData>(response.getWriter())
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(false)
                .build();

        //write all users to csv file
        writer.write(dataService.getUCCUsersData());
	}
        
        /**
         * 
         * @return 
         */
        @RequestMapping(value = "/customers", method = RequestMethod.GET)
	public Collection<UCCUserData> getUsersData() {
		return dataService.getUCCUsersData();
	}

        /**
         * 
         * @param nin
         * @return 
         */
	@RequestMapping(value = "/customer/{nin}", method = RequestMethod.GET)
	public UCCUserData getUserData(@PathVariable("nin") String nin) {
		return dataService.getUCCUserData(nin);
	}
        
        /**
         * 
         * @param msisdn
     * @param response
         * @return 
         */
	@RequestMapping(value = "/customer", method = RequestMethod.GET)
	public UCCUserData getUserDataByMsisdn(@RequestParam(name = "msisdn") String msisdn,
                HttpServletResponse response) throws Exception{
            if(msisdn == null || msisdn.length() < 12) {
                response.sendError(400, "Invalid MSISDN");
                return null;
            }
            if(!msisdn.contains("+")){
                msisdn = "+"+msisdn.trim();
            }
            return dataService.getUCCUserDataByMsisdn(msisdn);
	}
	/**
         * API to get customers call data record in json format
         * @param request
         * @return 
         */
	@RequestMapping(value = "/cdr", method = RequestMethod.GET)
	public ResponseEntity<Collection<CDRData>> getCDRData(@RequestBody CDRDataRequest request) {
		
		String fromDate = request.getFromDate();
		String toDate = request.getToDate();
		
		if(fromDate == null || fromDate.equals("") || toDate == null || toDate.equals("")) {
			return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		}
		
		LOGGER.debug("called getCDRData. query parameter fromDate = "+fromDate+" and toDate = "+toDate);
		return new ResponseEntity<>(dataService.getCDRDetails(fromDate,toDate), HttpStatus.OK);
	}
        

        /**
         * API to get details of customer's call record in csv format.
         * @param request
         * @param response
         * @throws Exception 
         */
	@RequestMapping(value = "/cdr-csv", method = RequestMethod.GET)
	public void downloadCDRData(@RequestBody CDRDataRequest request, HttpServletResponse response) throws Exception {
		
		String fromDate = request.getFromDate();
		String toDate = request.getToDate();
                LOGGER.debug("called getCDRData. query parameter fromDate = "+fromDate+" and toDate = "+toDate);
		
		if(fromDate == null || fromDate.equals("") || toDate == null || toDate.equals("")) {
                    response.sendError(400, "Invalid Date Provided");
                    return;
		}
		
                String filename = "smile-cdr-data.csv";

                response.setContentType("text/csv");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");
        
                ColumnPositionMappingStrategy mapStrategy
                    = new ColumnPositionMappingStrategy();
                String[] columns = new String[]{"originator", "recipient", "startingTime","duration","type"};
                mapStrategy.setColumnMapping(columns);
                mapStrategy.setType(CDRData.class);
                mapStrategy.generateHeader(CDRData.class);
                //create a csv writer
                StatefulBeanToCsv<CDRData> writer = new StatefulBeanToCsvBuilder<CDRData>(response.getWriter())
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .withMappingStrategy(mapStrategy)
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .withOrderedResults(false)
                    .build();
                //write all users to csv file
                writer.write(dataService.getCDRDetails(fromDate,toDate));
	}
}
