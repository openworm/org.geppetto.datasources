/**
 * 
 */
package org.geppetto.datasources.neo4j;

import java.util.List;
import java.util.Map;

import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;

/**
 * @author matteocantarelli
 *
 */
public class Neo4jResponseProcessor implements IQueryResponseProcessor
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.datasources.IQueryResponseProcessor#processResponse(java.lang.String)
	 */
	@Override
	public QueryResults processResponse(Map<String, Object> response) throws GeppettoDataSourceException
	{
		QueryResults results = DatasourcesFactory.eINSTANCE.createQueryResults();
		if(((List) response.get("results")).size() > 0)
		{
			List<String> headers = (List<String>) ((List) ((Map<String, Object>) ((List) response.get("results")).get(0)).get("columns"));

			results.getHeader().addAll(headers);

			results.getResults().clear();
			List<Map<String, Object>> data = (List<Map<String, Object>>) ((List) ((Map<String, Object>) ((List) response.get("results")).get(0)).get("data"));
			for(Map<String, Object> rowObject : data)
			{
				QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
				List<Object> row = (List<Object>) rowObject.get("row");
				for(Object value : row)
				{
					resultRow.getValues().add(value);
				}
				results.getResults().add(resultRow);
			}
		}
		if(((List) response.get("errors")).size() > 0){
			throw new GeppettoDataSourceException(((List) response.get("errors")).toString());
		}

		return results;
	}

}
