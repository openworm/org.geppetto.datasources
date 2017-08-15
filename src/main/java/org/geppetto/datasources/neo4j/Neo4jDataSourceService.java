
package org.geppetto.datasources.neo4j;

import org.geppetto.datasources.ADataSourceService;
import org.geppetto.datasources.IQueryResponseProcessor;

/**
 * @author matteocantarelli
 *
 */
public class Neo4jDataSourceService extends ADataSourceService
{

	public Neo4jDataSourceService()
	{
		super("/templates/neo4j/queryTemplate.vm");
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.datasources.ADataSourceService#getConnectionType()
	 */
	@Override
	public ConnectionType getConnectionType()
	{
		return ConnectionType.POST;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.datasources.ADataSourceService#getQueryResponseProcessor()
	 */
	@Override
	public IQueryResponseProcessor getQueryResponseProcessor()
	{
		if(queryResponseProcessor == null)
		{
			queryResponseProcessor = new Neo4jResponseProcessor();
		}
		return queryResponseProcessor;
	}

}
