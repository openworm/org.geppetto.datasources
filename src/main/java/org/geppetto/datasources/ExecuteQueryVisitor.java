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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.geppetto.core.common.GeppettoHTTPClient;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.JSONUtility;
import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.datasources.IQueryProcessor;
import org.geppetto.core.datasources.QueryChecker;
import org.geppetto.core.datasources.VelocityUtils;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.services.ServiceCreator;
import org.geppetto.model.datasources.AQueryResult;
import org.geppetto.model.datasources.CompoundQuery;
import org.geppetto.model.datasources.CompoundRefQuery;
import org.geppetto.model.datasources.DataSource;
import org.geppetto.model.datasources.ProcessQuery;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.datasources.SerializableQueryResult;
import org.geppetto.model.datasources.SimpleQuery;
import org.geppetto.model.datasources.util.DatasourcesSwitch;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;
import com.google.gson.JsonSyntaxException;

/**
 * @author matteocantarelli
 */
public class ExecuteQueryVisitor extends DatasourcesSwitch<Object>
{

	private static final String ID = "ID";

	private boolean count = false; // true if we want to execute a count

	private QueryResults results = null;

	private Variable variable;

	private Map<String, Object> processingOutputMap = new HashMap<String, Object>();

	private GeppettoModelAccess geppettoModelAccess;

	private int resultsCount = -1;

	public ExecuteQueryVisitor(Variable variable, GeppettoModelAccess geppettoModelAccess)
	{
		this.variable = variable;
		this.geppettoModelAccess = geppettoModelAccess;
	}

	private ADataSourceService getDataSourceService(Query query) throws GeppettoInitializationException
	{
		DataSource dataSource = getDataSource(query);
		ADataSourceService dataSourceService = (ADataSourceService) ServiceCreator.getNewServiceInstance(((DataSource) dataSource).getDataSourceService());
		dataSourceService.initialize((DataSource) dataSource, geppettoModelAccess);
		return dataSourceService;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.util.GeppettoSwitch#caseProcessQuery(org.geppetto.model.ProcessQuery)
	 */
	@Override
	public Object caseProcessQuery(ProcessQuery query)
	{
		if(!count || (count && query.isRunForCount()))
		{
			if(QueryChecker.check(query, getVariable()))
			{
				try
				{
					IQueryProcessor queryProcessor = (IQueryProcessor) ServiceCreator.getNewServiceInstance(query.getQueryProcessorId());
					this.results = queryProcessor.process(query, getDataSource(query), getVariable(), getResults(), geppettoModelAccess);
					this.processingOutputMap = queryProcessor.getProcessingOutputMap();
				}
				catch(GeppettoInitializationException e)
				{
					return new GeppettoVisitingException(e);
				}
				catch(GeppettoDataSourceException e)
				{
					return new GeppettoVisitingException(e);
				}
			}
		}
		return super.caseProcessQuery(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.datasources.util.DatasourcesSwitch#caseCompoundQuery(org.geppetto.model.datasources.CompoundQuery)
	 */
	@Override
	public Object caseCompoundQuery(CompoundQuery query)
	{
		if(!count || (count && query.isRunForCount()))
		{
			ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(variable, geppettoModelAccess);
			runQueryVisitor.processingOutputMap.putAll(processingOutputMap);

			try
			{
				GeppettoModelTraversal.applyDirectChildrenOnly(query, runQueryVisitor);
				mergeResults(runQueryVisitor.getResults());
			}
			catch(GeppettoVisitingException e)
			{
				return e;
			}
			catch(GeppettoDataSourceException e)
			{
				return new GeppettoVisitingException(e);
			}
		}
		return super.caseCompoundQuery(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.datasources.util.DatasourcesSwitch#caseCompoundRefQuery(org.geppetto.model.datasources.CompoundRefQuery)
	 */
	@Override
	public Object caseCompoundRefQuery(CompoundRefQuery compoundQuery)
	{
		if(!count || (count && compoundQuery.isRunForCount()))
		{
			for(Query query : compoundQuery.getQueryChain())
			{
				this.doSwitch(query);
			}
		}
		return super.caseCompoundRefQuery(compoundQuery);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.util.GeppettoSwitch#caseSimpleQuery(org.geppetto.model.SimpleQuery)
	 */
	@Override
	public Object caseSimpleQuery(SimpleQuery query)
	{
		if(!count || (count && query.isRunForCount()))
		{
			try
			{
				if(QueryChecker.check(query, getVariable()))
				{
					ADataSourceService dataSourceService = getDataSourceService(query);
					String url = getDataSource(query).getUrl();
					String queryString = null;
					if(count)
					{
						queryString = !query.getCountQuery().isEmpty() ? query.getCountQuery() : query.getQuery();
					}
					else
					{
						queryString = query.getQuery();
					}

					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put(ID, getVariable().getId());
					properties.put("QUERY", queryString);

					if(processingOutputMap != null)
					{
						properties.putAll(processingOutputMap);
					}

					String processedQueryString = VelocityUtils.processTemplate(dataSourceService.getTemplate(), properties);

					String response = null;
					switch(dataSourceService.getConnectionType())
					{
						case GET:
							response = GeppettoHTTPClient.doGET(url, processedQueryString);
							break;
						case POST:
							response = GeppettoHTTPClient.doJSONPost(url, processedQueryString);
							break;
					}

					processResponse(response, dataSourceService);
				}
			}
			catch(GeppettoDataSourceException e)
			{
				return new GeppettoVisitingException(e);
			}
			catch(GeppettoInitializationException e)
			{
				return new GeppettoVisitingException(e);
			}
		}
		return super.caseSimpleQuery(query);
	}

	/**
	 * @param response
	 * @throws GeppettoDataSourceException
	 */
	private void processResponse(String response, ADataSourceService dataSourceService) throws GeppettoDataSourceException
	{
		try{
			if(count)
			{
				Map<String, Object> responseMap = JSONUtility.getAsMap(response);
				results = dataSourceService.getQueryResponseProcessor().processResponse(responseMap);
				// TODO How to get the count if it is actually specified?
			}
			else
			{
				Map<String, Object> responseMap = JSONUtility.getAsMap(response);
				results = dataSourceService.getQueryResponseProcessor().processResponse(responseMap);
			}
		}catch (JsonSyntaxException e){
			System.out.println("JsonSyntaxException handling: " + response);
			System.out.println(e);
			throw new GeppettoDataSourceException(e);
		}
	}

	/**
	 * @param processResponse
	 * @throws GeppettoDataSourceException
	 */
	private void mergeResults(QueryResults processedResults) throws GeppettoDataSourceException
	{

		if(results != null)
		{
			if(!results.getHeader().contains(ID) || !processedResults.getHeader().contains(ID))
			{
				throw new GeppettoDataSourceException("Cannot merge without an ID in the results");
			}

			int baseId = results.getHeader().indexOf(ID);
			int mergeId = processedResults.getHeader().indexOf(ID);

			for(String column : processedResults.getHeader())
			{
				if(!column.equals(ID))
				{
					results.getHeader().add(column);
				}
			}

			for(AQueryResult result : results.getResults())
			{
				String currentId = ((SerializableQueryResult) result).getValues().get(baseId);
				for(AQueryResult mergeResult : processedResults.getResults())
				{
					if(((SerializableQueryResult) mergeResult).getValues().get(mergeId).equals(currentId))
					{
						// we are in the right row
						for(String column : processedResults.getHeader())
						{
							if(!column.equals(ID))
							{
								int columnId = processedResults.getHeader().indexOf(column);
								((SerializableQueryResult) result).getValues().add(((SerializableQueryResult) mergeResult).getValues().get(columnId));
							}
						}
						break;
					}
				}
			}
		}
		else
		{
			results = processedResults;
		}

	}

	/**
	 * @param query
	 * @return
	 */
	private DataSource getDataSource(Query query)
	{
		EObject parent = query.eContainer();
		while(!(parent instanceof DataSource))
		{
			parent = parent.eContainer();
		}
		return (DataSource) parent;
	}

	/**
	 * @return
	 */
	private Variable getVariable()
	{
		return variable;
	}

	/**
	 * @return
	 */
	public QueryResults getResults()
	{
		return results;
	}

	public int getCount()
	{
		return resultsCount;
	}

	/**
	 * @param countOnly
	 */
	public void countOnly(boolean countOnly)
	{
		count = countOnly;

	}


}
