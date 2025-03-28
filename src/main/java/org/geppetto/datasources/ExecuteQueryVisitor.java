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
import org.geppetto.datasources.neo4j.Neo4jDataSourceService;
import org.geppetto.datasources.solr.SOLRdataSourceService;

/**
 * @author matteocantarelli
 */
public class ExecuteQueryVisitor extends DatasourcesSwitch<Object>
{

	private static final String ID = "ID";

	private boolean count = false; // true if we want to execute a count

	private QueryResults results = null;
	private QueryResults mergedResults = DatasourcesFactory.eINSTANCE.createQueryResults();

	private Variable variable;

	private Map<String, Object> processingOutputMap = new HashMap<String, Object>();

	private GeppettoModelAccess geppettoModelAccess;

	private int resultsCount = -1;

    // Add these fields
    private int pageSize = 1000; // Default page size
    private int currentPage = 0;
    private boolean paginated = false;
    private int totalResults = 0;
    private boolean hasMorePages = false;

    private Query originalQuery; // Add this field to store the original query

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
					System.out.println("DEBUG: About to get QueryProcessor instance for ID: " + query.getQueryProcessorId());
					IQueryProcessor queryProcessor = (IQueryProcessor) ServiceCreator.getNewServiceInstance(query.getQueryProcessorId());
					System.out.println("DEBUG: QueryProcessor instance created successfully. About to process query...");
					this.results = queryProcessor.process(query, getDataSource(query), getVariable(), getResults(), geppettoModelAccess);
					System.out.println("DEBUG: Query processed successfully");
					this.processingOutputMap = queryProcessor.getProcessingOutputMap();
					System.out.println("DEBUG: Processing output map retrieved");
				}
				catch(GeppettoInitializationException e)
				{
					System.out.println("DEBUG: GeppettoInitializationException occurred: " + e.getMessage());
					e.printStackTrace();
					return new GeppettoVisitingException(e);
				}
				catch(GeppettoDataSourceException e)
				{
					System.out.println("DEBUG: GeppettoDataSourceException occurred: " + e.getMessage());
					e.printStackTrace();
					return new GeppettoVisitingException(e);
				}
				catch(Exception e)
				{
					System.out.println("DEBUG: Unexpected exception occurred: " + e.getClass().getName() + ": " + e.getMessage());
					e.printStackTrace();
					return new GeppettoVisitingException(new GeppettoDataSourceException(e));
				}
			}
		}
		try {
			return super.caseProcessQuery(query);
		} catch (Exception e) {
			System.out.println("DEBUG: Exception occurred in super.caseProcessQuery: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			return new GeppettoVisitingException(new GeppettoDataSourceException(e));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.model.datasources.util.DatasourcesSwitch#caseCompoundQuery(org.geppetto.model.datasources.CompoundQuery)
	 */
	@Override
	public Object caseCompoundQuery(CompoundQuery query)
	{
		System.out.println("DEBUG: Entering caseCompoundQuery for query: " + query.getId());
		if(!count || (count && query.isRunForCount()))
		{
			System.out.println("DEBUG: Creating new ExecuteQueryVisitor");
			ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(variable, geppettoModelAccess);
			
			System.out.println("DEBUG: Copying processing output map");
			runQueryVisitor.processingOutputMap.putAll(processingOutputMap);

			try
			{
				System.out.println("DEBUG: Before applying GeppettoModelTraversal");
				GeppettoModelTraversal.applyDirectChildrenOnly(query, runQueryVisitor);
				System.out.println("DEBUG: After applying GeppettoModelTraversal");
				
				System.out.println("DEBUG: Before merging results");
				mergeResults(runQueryVisitor.getResults());
				System.out.println("DEBUG: After merging results");
			}
			catch(GeppettoVisitingException e)
			{
				System.out.println("DEBUG: Caught GeppettoVisitingException: " + e.getMessage());
				e.printStackTrace();
				return e;
			}
			catch(GeppettoDataSourceException e)
			{
				System.out.println("DEBUG: Caught GeppettoDataSourceException: " + e.getMessage());
				e.printStackTrace();
				return new GeppettoVisitingException(e);
			}
			catch(Exception e)
			{
				System.out.println("DEBUG: Caught unexpected exception: " + e.getClass().getName() + ": " + e.getMessage());
				e.printStackTrace();
				return new GeppettoVisitingException(new GeppettoDataSourceException(e));
			}
		}
		else
		{
			System.out.println("DEBUG: Skipping query execution due to count flag");
		}
		System.out.println("DEBUG: Exiting caseCompoundQuery");
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
		System.out.println("DEBUG: Entering caseCompoundRefQuery for query chain");
		if(!count || (count && compoundQuery.isRunForCount()))
		{
			System.out.println("DEBUG: Processing query chain with " + compoundQuery.getQueryChain().size() + " queries");
			int queryIndex = 0;
			for(Query query : compoundQuery.getQueryChain())
			{
				System.out.println("DEBUG: About to process query #" + (++queryIndex) + " in chain");
				try {
					this.doSwitch(query);
					System.out.println("DEBUG: Successfully processed query #" + queryIndex);
				} catch (Exception e) {
					System.out.println("DEBUG: Exception while processing query #" + queryIndex + ": " + e.getMessage());
					e.printStackTrace();
					throw e; // Re-throw to maintain original behavior
				}
			}
			System.out.println("DEBUG: Completed processing all queries in chain");
		}
		else
		{
			System.out.println("DEBUG: Skipping query chain execution due to count flag");
		}
		System.out.println("DEBUG: Exiting caseCompoundRefQuery");
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
		// Store the original query at the beginning of this method
		this.originalQuery = query;
		
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
						// Add pagination parameters if enabled
						if(paginated) {
							// Modify query string based on data source type
							queryString = addPaginationToQuery(queryString, dataSourceService);
						}
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
				
				// Update pagination information
				if(paginated && dataSourceService instanceof SOLRdataSourceService) {
					// For SOLR
					if(responseMap.containsKey("response")) {
						Map<String, Object> respObj = (Map<String, Object>)responseMap.get("response");
						if(respObj.containsKey("numFound")) {
							totalResults = ((Number)respObj.get("numFound")).intValue();
							hasMorePages = (currentPage + 1) * pageSize < totalResults;
						}
					}
				}
				// Add similar handling for other data sources
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
		// if this arrives from a first query results should be empty, so we automatically assign 
		// processedResults to results
		if(results != null)
		{
			if(!results.getHeader().contains(ID) || !processedResults.getHeader().contains(ID))
			{
				throw new GeppettoDataSourceException("Cannot merge without an ID in the results");
			}
			
			Set<String> idsList = new HashSet<String>();
			
			// Extract the index of the id for each list of results
			int baseId = results.getHeader().indexOf(ID);
			int mergeId = processedResults.getHeader().indexOf(ID);
			
			// add all the ids from results and processedResults to idsList, a Set that will contain all
			// unique ids that we can iterate to do a merge of the data
			for(AQueryResult result : results.getResults())
			{
				idsList.add(((SerializableQueryResult) result).getValues().get(baseId));
			}
			
			for(AQueryResult result : processedResults.getResults())
			{
				idsList.add(((SerializableQueryResult) result).getValues().get(mergeId));
			}

			// Extract all the headers contained in results and processedResults and put all in mergedResults
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
			
			int lastId = mergedResults.getHeader().indexOf(ID);
			
			for(String id : idsList) {
				// This is the real deal, here we iterate all the ids and for each id
				Boolean resultAdded = false;
				SerializableQueryResult newRecord = null;
				for(AQueryResult result : results.getResults()) {
					// if the id is found in one of the records contained in results then we set newRecord
					// to the result found
					if(((SerializableQueryResult) result).getValues().get(baseId).equals(id)) {
						newRecord = (SerializableQueryResult) result;
					}
				}

				// Then we check the same id in processedResults
				for(AQueryResult result : processedResults.getResults()) {
					// If this is found 
					if(((SerializableQueryResult) result).getValues().get(mergeId).equals(id)) {
						// and was not found in the results iteration, then newRecord will be set to this result
						if(newRecord == null) {
							newRecord = (SerializableQueryResult) result;
						// differently we iterate this results per column and we add whatever is present here
						// that was not present in the previous check, keep in mind that we overwrite also the
						// columns that were already present
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
				
				// Finally we check if this id is present also in mergedResults, that carry over all the results
				// from previous queries/compound, if this was already present then we overwrite all the
				// previous informations with the coming one
				for(AQueryResult result : mergedResults.getResults()) {
					if((((SerializableQueryResult) result).getValues().get(lastId).equals(id)) && newRecord != null) {
						for(String column : mergedResults.getHeader())
							{
								if(!column.equals(ID))
								{
									int columnId = mergedResults.getHeader().indexOf(column);
									((SerializableQueryResult) result).getValues().add(((SerializableQueryResult) newRecord).getValues().get(columnId));
								}
							}
						resultAdded = true;
						break;
					}
				}
				
				// Instead if the id was not present in mergedResult we simply add this record
				if(!resultAdded) { 
					mergedResults.getResults().add(newRecord);
				}
			}
			// Then we re-initialize results as mergedResults that contains all the results to pass to frontend
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

    /**
     * Add pagination parameters to query based on data source type
     */
    private String addPaginationToQuery(String query, ADataSourceService dataSourceService) {
        String paginatedQuery = query;
        
        // Different data sources might need different pagination formats
        if(dataSourceService instanceof Neo4jDataSourceService) {
            // Add Neo4j-specific pagination
            paginatedQuery += " SKIP " + (currentPage * pageSize) + " LIMIT " + pageSize;
        } else if(dataSourceService instanceof SOLRdataSourceService) {
            // Add SOLR-specific pagination
            if(paginatedQuery.contains("start=")) {
                // Replace existing start parameter
                paginatedQuery = paginatedQuery.replaceAll("start=\\d+", "start=" + (currentPage * pageSize));
            } else {
                paginatedQuery += "&start=" + (currentPage * pageSize);
            }
            
            if(paginatedQuery.contains("rows=")) {
                // Replace existing rows parameter
                paginatedQuery = paginatedQuery.replaceAll("rows=\\d+", "rows=" + pageSize);
            } else {
                paginatedQuery += "&rows=" + pageSize;
            }
        }
        // Add similar conditions for other data source types
        
        return paginatedQuery;
    }

    /**
     * Enable pagination with specified page size
     */
    public void enablePagination(int pageSize) {
        this.paginated = true;
        this.pageSize = pageSize;
    }

    /**
     * Set the current page to retrieve
     */
    public void setPage(int page) {
        this.currentPage = page;
    }

    /**
     * Get the total number of results
     */
    public int getTotalResults() {
        return totalResults;
    }

    /**
     * Check if more pages are available
     */
    public boolean hasMorePages() {
        return hasMorePages;
    }

    /**
     * Get total number of pages
     */
    public int getTotalPages() {
        return (totalResults + pageSize - 1) / pageSize;
    }

    public void streamResults(QueryResultConsumer consumer) throws GeppettoDataSourceException {
        if (originalQuery == null) {
            throw new GeppettoDataSourceException("No query to execute for streaming");
        }
        
        int page = 0;
        boolean hasMore = true;
        
        while(hasMore) {
            setPage(page);
            this.results = null;  // Reset results
            this.mergedResults = DatasourcesFactory.eINSTANCE.createQueryResults();
            
            // Execute query for current page
            doSwitch(originalQuery);  // Use originalQuery instead of theOriginalQuery
            
            // Process this page of results
            if(results != null && !results.getResults().isEmpty()) {
                consumer.processResults(results);
            } else {
                hasMore = false;
            }
            
            hasMore = hasMore && hasMorePages;
            page++;
        }
    }

    public interface QueryResultConsumer {
        void processResults(QueryResults pageResults) throws GeppettoDataSourceException;
    }

}
