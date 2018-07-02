/**
 * 
 */
package org.geppetto.datasources.owlery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;


import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;

/**
 * @author dariodelpiano
 *
 */
public class OWLeryResponseProcessor implements IQueryResponseProcessor
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
		List<String> headers = new ArrayList<String>();

		for(String keyExtracted : response.keySet()) {
			if(!headers.contains(keyExtracted)) {
				headers.add(keyExtracted);
			}
		}
		
		results.getHeader().addAll(headers);
		QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
		
		for(String column : headers) { 
			if(response.containsKey(column)) {
				resultRow.getValues().add((response.get(column)));
			}
			else {
				resultRow.getValues().add(null);
			}
		}
		
		results.getResults().add(resultRow);
		
		return results;
	}

}
