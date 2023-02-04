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
		Map<String, Object> data = (Map<String, Object>) response.get("response");
		double numFound =  (double)data.get("numFound");
		if(numFound > 0)
		{
			Set<String> headers = new HashSet<String>();
			String queryName = null;
			results.getResults().clear();
			List<Object> rowObject = (List<Object>) data.get("docs");
			Object terminfo = rowObject.get(0);

			QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
			if (queryName == null) {
				Set<String> keySet = data.keySet();
				keySet.remove("id");
				keySet.remove("_version_");
				queryName = keySet.toString();
			}
			if (headers.size() < 1) {
				headers.add("JSON");
			}
			resultRow.getValues().add(terminfo);
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
