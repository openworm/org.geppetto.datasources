/**
 * 
 */
package org.geppetto.datasources.solr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Robbie1977
 *
 */
public class SOLRresponseProcessor implements IQueryResponseProcessor
{
	// This class is used to process the response from SOLR. It is called from the SOLRQuery class.
	// The response is passed in as a Map<String, Object> and the results are returned as a QueryResults object.
	// This class creates the QueryResults object and adds rows to it, one row for each object in the response.
	// Each row is a QueryResult object that contains a list of values (objects).
	// Each value is a field in the SOLR response. 
	// The headers for the QueryResults object are also created here.
	// The headers are the field names in the SOLR response.
	// The headers are added to the QueryResults object.
	// The QueryResults object is returned to the caller.
	// If the response is empty, the QueryResults object is empty and the headers are empty.
	// This class is used by the SOLRQuery class.

	public QueryResults processResponse(Map<String, Object> response)
	{
		QueryResults results = DatasourcesFactory.eINSTANCE.createQueryResults();
		Map<String, Object> data = (Map<String, Object>) response.get("response");
		double numFound =  (double)data.get("numFound");
		if(numFound > 0)
		{
			results.getResults().clear();
			List<Object> rowObject = (List<Object>) data.get("docs");
			Object terminfo = rowObject.get(0);
			Gson gsonObj = new Gson();
			String jsonStr = gsonObj.toJson(terminfo);
			JsonParser parser = new JsonParser();
			JsonObject jsonFormat = (JsonObject) parser.parse(jsonStr);
			JsonArray termInfo = (JsonArray) jsonFormat.get("term_info");
			String json = termInfo.get(0).getAsString();
			jsonFormat = (JsonObject) parser.parse(json);
			
			QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
			resultRow.getValues().add(jsonFormat);
			results.getResults().add(resultRow);
			results.getHeader().add("term_info");
		}
		else
		{
			// TODO: report reponse to log
		}
		return results;
	}
}
