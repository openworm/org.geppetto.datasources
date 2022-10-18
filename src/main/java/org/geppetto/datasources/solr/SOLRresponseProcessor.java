/**
 * 
 */
package org.geppetto.datasources.solr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			Set<String> headers = new HashSet<String>();
			String queryName = null;
			results.getResults().clear();
			List<Map<String, Object>> data = (List<Map<String, Object>>) ((List) ((Map<String, Object>) ((List) response.get("response")).get(0)).get("data"));
			for(Map<String, Object> rowObject : data)
			{
				QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
				if (queryName == null) {
					Set<String> keySet = rowObject.keySet();
					keySet.remove("id");
					keySet.remove("_version_");
					queryName = keySet.toString();
				}
				if (headers.size() < 1) {
					headers.add("JSON");
				}
				results.getResults().add((String) rowObject.get(queryName));
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
