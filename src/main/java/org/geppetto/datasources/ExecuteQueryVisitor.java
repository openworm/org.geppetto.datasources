
package org.geppetto.datasources;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.geppetto.model.datasources.DatasourcesFactory;
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
			String processedQueryString = "";
			String url = "";
			try
			{
				if(QueryChecker.check(query, getVariable()))
				{
					ADataSourceService dataSourceService = getDataSourceService(query);
					url = getDataSource(query).getUrl();
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

					processedQueryString = VelocityUtils.processTemplate(dataSourceService.getTemplate(), properties);

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
				System.out.println("Query request: " + url + "?" + processedQueryString);
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
			String customJson = "";
			if(response.startsWith("[")) {
				/* Checking if the server returns an array and if that's the case adding a dummy response 
				 * object to fit the JSON into our standard Map<String, Object> representation. 
				 * Regarding the String manipulation although ugly this is the most performant way of performing 
				 * this task. */
				customJson = "{\"response\":"+response+"}";
				response = customJson;
			}
			
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
			
			QueryResults mergedResults = DatasourcesFactory.eINSTANCE.createQueryResults();
			Set<String> idsList = new HashSet<String>();
			
			int baseId = results.getHeader().indexOf(ID);
			int mergeId = processedResults.getHeader().indexOf(ID);
			
			for(AQueryResult result : results.getResults())
			{
				idsList.add(((SerializableQueryResult) result).getValues().get(baseId));
			}
			
			for(AQueryResult result : processedResults.getResults())
			{
				idsList.add(((SerializableQueryResult) result).getValues().get(mergeId));
			}

			for(String column : processedResults.getHeader())
			{
				if(!column.equals(ID))
				{
					results.getHeader().add(column);
				}
			}
			
			for(String column : results.getHeader())
			{
				mergedResults.getHeader().add(column);
			}
			
			for(String id : idsList) {
				SerializableQueryResult newRecord = null;
				for(AQueryResult result : results.getResults()) {
					if(((SerializableQueryResult) result).getValues().get(baseId).equals(id)) {
						newRecord = (SerializableQueryResult) result;
					}
				}
				
				for(AQueryResult result : processedResults.getResults()) {
					if(((SerializableQueryResult) result).getValues().get(mergeId).equals(id)) {
						if(newRecord == null) {
							newRecord = (SerializableQueryResult) result;
						} else {
							for(String column : processedResults.getHeader())
							{
								if(!column.equals(ID))
								{
									int columnId = processedResults.getHeader().indexOf(column);
									((SerializableQueryResult) newRecord).getValues().add(((SerializableQueryResult) result).getValues().get(columnId));
								}
							}
							break;
						}
					}
				}
				
				mergedResults.getResults().add(newRecord);
			}
			results = mergedResults;
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
