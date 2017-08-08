
package org.geppetto.datasources.aberowl;

import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.datasources.IQueryListener;
import org.geppetto.datasources.ADataSourceService;
import org.geppetto.datasources.ExecuteQueryVisitor;
import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;

/**
 * @author matteocantarelli
 *
 */
public class AberOWLDataSourceService extends ADataSourceService
{

	public AberOWLDataSourceService()
	{
		super("/templates/aberOWL/queryTemplate.vm");
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.datasources.ADataSourceService#getConnectionType()
	 */
	@Override
	public ConnectionType getConnectionType()
	{
		return ConnectionType.GET;
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
			queryResponseProcessor = new AberOWLResponseProcessor();
		}
		return queryResponseProcessor;
	}



}
