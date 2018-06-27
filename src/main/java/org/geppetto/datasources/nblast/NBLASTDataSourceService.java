
package org.geppetto.datasources.nblast;

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
 * @author dariodelpiano
 *
 */

public class NBLASTDataSourceService extends ADataSourceService
{

	public NBLASTDataSourceService()
	{
		super("/templates/NBLAST/queryTemplate.vm");
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
			queryResponseProcessor = new NBLASTResponseProcessor();
		}
		return queryResponseProcessor;
	}



}
