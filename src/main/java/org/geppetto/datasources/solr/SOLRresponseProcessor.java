/**
 * 
 */
package org.geppetto.datasources.solr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonObject;

import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;

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
		if(((Integer) ((Map<String, Object>) response.get("response")).get("numFound")) > 0)
		{
			List<String> headers = new ArrayList<String>();
			String queryName = null;
			results.getResults().clear();
			List<Map<String, Object>> data = (List<Map<String, Object>>) ((List) ((Map<String, Object>) ((List) response.get("response")).get(0)).get("data"));
			Set<String> headers = new HashSet<String>();
			for(Map<String, Object> rowObject : data)
			{
				QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
				if (queryName == null) {
					Set<String> keySet = rowObject.keySet();
					keySet.remove("id");
					keySet.remove("_version_");
					queryName = keySet.toString();
				}
				JsonObject row = new JsonObject((String) rowObject.get(queryName));
				Set<String> keySet = row.keySet();
				if (headers.size() < 1) {
					headers = row.keySet();
				}
				results.getResults().add(resultRow);
			}
			results.getHeader().addAll(headers);
		}
		else
		{
			// TODO: report reponse to log
		}
		return results;
	}

}
