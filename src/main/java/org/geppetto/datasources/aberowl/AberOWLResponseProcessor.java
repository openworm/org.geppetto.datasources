/**
 * 
 */
package org.geppetto.datasources.aberowl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.GeppettoFactory;
import org.geppetto.model.QueryResult;
import org.geppetto.model.QueryResults;

/**
 * @author matteocantarelli
 *
 */
public class AberOWLResponseProcessor implements IQueryResponseProcessor
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.datasources.IQueryResponseProcessor#processResponse(java.lang.String)
	 */
	@Override
	public QueryResults processResponse(Map<String, Object> response)
	{
		QueryResults results = GeppettoFactory.eINSTANCE.createQueryResults();
		List<String> headers = new ArrayList<String>();

		List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("result");

		//STEP 1 - Add all the headers
		for(Map<String, Object> rowObject : data)
		{
			for(String column : rowObject.keySet())
			{
				if(!headers.contains(column))
				{
					headers.add(column);
				}
			}
		}
		
		results.getHeader().addAll(headers);
		
		//STEP 2 - Add all the values at the right place
		for(Map<String, Object> rowObject : data)
		{
			QueryResult resultRow = GeppettoFactory.eINSTANCE.createQueryResult();
			for(String column : headers)
			{
				if(rowObject.containsKey(column))
				{
					resultRow.getValues().add((rowObject.get(column)));
				}
				else{
					resultRow.getValues().add(null);
				}
			}
			results.getResults().add(resultRow);
		}

		return results;
	}

}
