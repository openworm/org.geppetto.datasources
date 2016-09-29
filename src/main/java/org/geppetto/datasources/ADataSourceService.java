/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.datasources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.datasources.IDataSourceService;
import org.geppetto.core.datasources.QueryChecker;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.services.AService;
import org.geppetto.model.datasources.CompoundRefQuery;
import org.geppetto.model.datasources.DataSource;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.datasources.SimpleQuery;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;

/**
 * @author matteocantarelli
 *
 */
public abstract class ADataSourceService extends AService implements IDataSourceService
{

	public abstract ConnectionType getConnectionType();

	public abstract IQueryResponseProcessor getQueryResponseProcessor();

	public enum ConnectionType
	{
		GET, POST
	}

	protected IQueryResponseProcessor queryResponseProcessor;

	private DataSource configuration;

	private String dataSourceTemplate;

	private GeppettoModelAccess geppettoModelAccess;

	private LinkedHashMap<String, QueryResults> cachedResults = new LinkedHashMap<String, QueryResults>();

	public ADataSourceService(String dataSourceTemplate)
	{
		this.dataSourceTemplate = dataSourceTemplate;
	}

	@Override
	public void initialize(DataSource configuration, GeppettoModelAccess geppettoModelAccess)
	{
		this.configuration = configuration;
		this.geppettoModelAccess = geppettoModelAccess;
	}

	protected GeppettoModelAccess getGeppettoModelAccess()
	{
		return geppettoModelAccess;
	}

	protected String getTemplate()
	{
		return dataSourceTemplate;
	}

	/**
	 * @return the configuration for this DataSourceService
	 */
	protected DataSource getConfiguration()
	{
		return configuration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#getAvailableQueries(org.geppetto.model.variables.Variable)
	 */
	@Override
	public List<Query> getAvailableQueries(Variable variable)
	{
		List<Query> availableQueries = new ArrayList<Query>();
		for(Query query : configuration.getQueries())
		{
			if(QueryChecker.check(query, variable))
			{
				availableQueries.add(query);
			}
		}
		return availableQueries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#execute(org.geppetto.core.model.Query, org.geppetto.model.variables.Variable, org.geppetto.core.model.QueryListener)
	 */
	@Override
	public QueryResults execute(Query query, Variable variable) throws GeppettoDataSourceException
	{
		String key = getKey(query, variable);
		if(cachedResults.containsKey(key))
		{
			return cachedResults.get(key);
		}
		else
		{
			ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(variable, getGeppettoModelAccess());
			runQueryVisitor.doSwitch(query);
			QueryResults results = runQueryVisitor.getResults();
			cache(key, results);
			return results;
		}
	}

	/**
	 * @param key
	 * @param results
	 */
	private void cache(String key, QueryResults results)
	{
		if(cachedResults.size() > 10)
		{
			cachedResults.remove(cachedResults.keySet().iterator().next());
		}
		cachedResults.put(key, results);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#getNumberOfResults(org.geppetto.core.model.Query, org.geppetto.model.variables.Variable)
	 */
	@Override
	public int getNumberOfResults(Query query, Variable variable) throws GeppettoDataSourceException
	{
		int count = -1;
		if(query instanceof CompoundRefQuery)
		{
			SimpleQuery simpleQuery = (SimpleQuery) ((CompoundRefQuery) query).getQueryChain().get(0);
			ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(variable, getGeppettoModelAccess());
			if(simpleQuery.getCountQuery() != null && !simpleQuery.getCountQuery().isEmpty())
			{
				runQueryVisitor.countOnly(true);
				// Assumption: in a compound query the number of results is dictated only by the first simple query, any further processing or querying will not increase the number of results.
				// If this will change this algorithm has to change (and become a visitor)
				runQueryVisitor.doSwitch(simpleQuery);
				count = runQueryVisitor.getCount();
			}
			else
			{
				runQueryVisitor.countOnly(true);
				runQueryVisitor.doSwitch(query);
				QueryResults results = runQueryVisitor.getResults();
				// There is no query specified, we run everything and we cache it so if it will be actually run later one we'll already have the results
				count = results.getResults().size();

			}
		}
		return count;
	}

	/**
	 * @param query
	 * @param variable
	 * @return
	 */
	private String getKey(Query query, Variable variable)
	{
		return query.getPath() + ":" + variable.getPath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.IDataSource#fetchVariable(java.lang.String)
	 */
	@Override
	public void fetchVariable(String variableId) throws GeppettoDataSourceException
	{
		Variable fetchedVariable = VariablesFactory.eINSTANCE.createVariable();
		fetchedVariable.setId(variableId);
		getGeppettoModelAccess().addVariable(fetchedVariable);
		Query fetchVariableQuery = getConfiguration().getFetchVariableQuery();
		ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(fetchedVariable, getGeppettoModelAccess());
		runQueryVisitor.doSwitch(fetchVariableQuery);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.services.IService#registerGeppettoService()
	 */
	@Override
	public void registerGeppettoService() throws Exception
	{
		// Nothing to do here
	}

}
