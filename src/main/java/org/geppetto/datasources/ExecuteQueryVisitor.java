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
import java.util.HashMap;
import java.util.List;
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
import org.geppetto.datasources.ADataSourceService.ConnectionType;
import org.geppetto.model.datasources.AQueryResult;
import org.geppetto.model.datasources.CompoundRefQuery;
import org.geppetto.model.datasources.DataSource;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.ProcessQuery;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.datasources.SerializableQueryResult;
import org.geppetto.model.datasources.SimpleQuery;
import org.geppetto.model.datasources.util.DatasourcesSwitch;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;

/**
 * @author matteocantarelli Matteo TODO: I really want to move this to the datasource bundle...
 */
public class ExecuteQueryVisitor extends DatasourcesSwitch<Object>
{

	private DataSource dataSource = null;

	private boolean count; // true if we want to execute a count

	private QueryResults results = null;

	private Variable variable;

	private String dataSourceTemplate;

	private GeppettoModelAccess geppettoModelAccess;

	private ConnectionType connectionType;

	private IQueryResponseProcessor queryResponseProcessor;

	private Map<String, Object> processingOutputMap = new HashMap<String, Object>();

	/**
	 * 
	 */
	public ExecuteQueryVisitor(DataSource dataSource, String dataSourceTemplate, Variable variable, GeppettoModelAccess geppettoModelAccess, ConnectionType connectionType,
			IQueryResponseProcessor queryResponseProcessor)
	{
		this(dataSource, dataSourceTemplate, variable, geppettoModelAccess, false, connectionType, queryResponseProcessor);
	}

	/**
	 * @param variable
	 * @param count
	 * @param connectionType
	 * @param queryResponseProcessor
	 */
	public ExecuteQueryVisitor(DataSource dataSource, String dataSourceTemplate, Variable variable, GeppettoModelAccess geppettoModelAccess, boolean count, ConnectionType connectionType,
			IQueryResponseProcessor queryResponseProcessor)
	{
		super();
		this.dataSource = dataSource;
		this.geppettoModelAccess = geppettoModelAccess;
		this.dataSourceTemplate = dataSourceTemplate;
		this.variable = variable;
		this.count = count;
		this.connectionType = connectionType;
		this.queryResponseProcessor = queryResponseProcessor;
		results = DatasourcesFactory.eINSTANCE.createQueryResults();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.util.GeppettoSwitch#caseProcessQuery(org.geppetto.model.ProcessQuery)
	 */
	@Override
	public Object caseProcessQuery(ProcessQuery query)
	{
		if(QueryChecker.check(query, getVariable()))
		{
			try
			{
				IQueryProcessor queryProcessor = (IQueryProcessor) ServiceCreator.getNewServiceInstance(query.getQueryProcessorId());
				this.results = queryProcessor.process(query, dataSource, getVariable(), getResults(), geppettoModelAccess);
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
		return super.caseProcessQuery(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.datasources.util.DatasourcesSwitch#caseCompoundRefQuery(org.geppetto.model.datasources.CompoundRefQuery)
	 */
	@Override
	public Object caseCompoundRefQuery(CompoundRefQuery object)
	{
		for(Query query : object.getQueryChain())
		{
			this.doSwitch(query);
		}
		return super.caseCompoundRefQuery(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.util.GeppettoSwitch#caseSimpleQuery(org.geppetto.model.SimpleQuery)
	 */
	@Override
	public Object caseSimpleQuery(SimpleQuery query)
	{
		try
		{
			if(QueryChecker.check(query, getVariable()))
			{
				String url = getDataSource(query).getUrl();

				String queryString = count ? query.getCountQuery() : query.getQuery();

				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put("ID", getVariable().getId());
				properties.put("QUERY", queryString);

				if(processingOutputMap != null)
				{
					properties.putAll(processingOutputMap);
				}

				String processedQueryString = VelocityUtils.processTemplate(dataSourceTemplate, properties);

				String response = null;
				switch(connectionType)
				{
					case GET:
						response = GeppettoHTTPClient.doGET(url, processedQueryString);
						break;
					case POST:
						response = GeppettoHTTPClient.doJSONPost(url, processedQueryString);
						break;
				}

				processResponse(response);
			}
		}
		catch(GeppettoDataSourceException e)
		{
			return new GeppettoVisitingException(e);
		}
		return super.caseSimpleQuery(query);
	}

	/**
	 * @param response
	 */
	private void processResponse(String response)
	{
		// TODO Maybe split in two different visitors?
		if(count)
		{
			// TODO update the count
		}
		else
		{
			Map<String, Object> responseMap = JSONUtility.getAsMap(response);
			mergeResults(queryResponseProcessor.processResponse(responseMap));
		}

	}

	/**
	 * @param processResponse
	 */
	private void mergeResults(QueryResults processResponse)
	{
		results = processResponse;
//		if(results != null)
//		{
//			
//
//			processedResults.getHeader().add("ID");
//			processedResults.getHeader().add("Name");
//			processedResults.getHeader().add("Definition");
//
//			List<String> ids=new ArrayList<String>();
//			for(AQueryResult result : results.getResults())
//			{
//				SerializableQueryResult processedResult = DatasourcesFactory.eINSTANCE.createSerializableQueryResult();
//				processedResult.getValues().add(((QueryResult) result).getValues().get(idIndex).toString());
//				String id=((List<String>) ((QueryResult) result).getValues().get(nameIndex)).get(0);
//				processedResult.getValues().add(id);
//				ids.add(id);
//				processedResult.getValues().add(((QueryResult) result).getValues().get(descirptionIndex).toString());
//				processedResults.getResults().add(processedResult);
//			}
//			
//			processingOutputMap.put("ARRAY_ID_RESULTS",ids);
//		}
//		else
//		{
//			results = processResponse;
//		}

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
		// TODO Auto-generated method stub
		return 0;
	}

}
