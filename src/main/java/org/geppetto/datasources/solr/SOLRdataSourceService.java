
package org.geppetto.datasources.solr;

import org.geppetto.datasources.ADataSourceService;
import org.geppetto.datasources.IQueryResponseProcessor;

/**
 * @author matteocantarelli
 *
 */
public class SOLRdataSourceService extends ADataSourceService
{

	public SOLRdataSourceService()
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
			queryResponseProcessor = new SOLRresponseProcessor();
		}
		return queryResponseProcessor;
	}

}
