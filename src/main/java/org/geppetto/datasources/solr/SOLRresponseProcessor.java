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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.datasources.IQueryResponseProcessor#processResponse(java.lang.String)
	 */
	@Override
	public QueryResults processResponse(Map<String, Object> response)
	{
		QueryResults results = DatasourcesFactory.eINSTANCE.createQueryResults();
		Map<String, Object> data = (Map<String, Object>) response.get("response");
		double numFound =  (double)data.get("numFound");
		if(numFound > 0)
		{
			List<String> headers = new ArrayList<String>();
			String queryName = null;
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
			if (queryName == null) {
				Set<String> keySet = data.keySet();
				keySet.remove("id");
				keySet.remove("_version_");
				queryName = keySet.toString();
			}
			if (headers.size() < 1) {
				Set<Entry<String, JsonElement>> keySet = jsonFormat.entrySet();	
				
				for (Entry<String, JsonElement> entry : keySet) {
					headers.add(entry.getKey());
					JsonElement obj = entry.getValue();
					resultRow.getValues().add(obj);
				}
			}
			results.getResults().add(resultRow);
			results.getHeader().addAll(headers);
		}
		else
		{
			// TODO: report reponse to log
		}
		return results;
	}

}
