
package org.geppetto.datasources;

import org.geppetto.datasources.neo4j.Neo4jResponseProcessor;

/**
 * @author matteocantarelli
 *
 */
public class TestDataSourceService extends ADataSourceService
{

	public TestDataSourceService()
	{
		super("");
	}


	@Override
	public ConnectionType getConnectionType()
	{
		return ConnectionType.POST;
	}

	/* (non-Javadoc)
	 * @see org.geppetto.datasources.ADataSourceService#getQueryResponseProcessor()
	 */
	@Override
	public IQueryResponseProcessor getQueryResponseProcessor()
	{
		if(queryResponseProcessor==null){
			queryResponseProcessor=new Neo4jResponseProcessor();
		}
		return queryResponseProcessor;
	}

}
