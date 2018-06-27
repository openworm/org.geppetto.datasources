
package org.geppetto.datasources.nblast;

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
public class NBLASTResponseProcessor implements IQueryResponseProcessor
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
		List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("response");

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

		for(Map<String, Object> rowObject : data)
		{
			QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
			for(String column : headers)
			{
				if(rowObject.containsKey(column))
				{
					resultRow.getValues().add((rowObject.get(column)));
				}
				else
				{
					resultRow.getValues().add(null);
				}
			}
			results.getResults().add(resultRow);
		}

		return results;
	}
}
