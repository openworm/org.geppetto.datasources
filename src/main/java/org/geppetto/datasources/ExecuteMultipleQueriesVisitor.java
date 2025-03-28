package org.geppetto.datasources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.model.datasources.AQueryResult;
import org.geppetto.model.datasources.BooleanOperator;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.datasources.RunnableQuery;
import org.geppetto.model.datasources.SerializableQueryResult;
import org.geppetto.model.datasources.util.DatasourcesSwitch;
import org.geppetto.model.util.GeppettoModelException;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;

/**
 * @author matteocantarelli
 */
public class ExecuteMultipleQueriesVisitor extends DatasourcesSwitch<Object>
{

	private GeppettoModelAccess geppettoModelAccess;

	// Stores all the IDS returned by a given query
	private Map<QueryResults, List<String>> ids = new LinkedHashMap<QueryResults, List<String>>();
	// Stores all the operators
	private Map<QueryResults, BooleanOperator> results = new LinkedHashMap<QueryResults, BooleanOperator>();

	private List<String> finalIds = new ArrayList<String>();
	private QueryResults finalResults = DatasourcesFactory.eINSTANCE.createQueryResults();

	private Map<String, QueryResults> cachedResults;
	private Map<String, List<String>> cachedIds;

	private static final String ID = "ID";

	private int pageSize = -1;
	private int page = 0;
	private boolean paginated = false;

	public ExecuteMultipleQueriesVisitor(GeppettoModelAccess geppettoModelAccess, Map<String, QueryResults> cachedResults, Map<String, List<String>> cachedIds)
	{
		this.geppettoModelAccess = geppettoModelAccess;
		this.cachedResults = cachedResults;
		this.cachedIds = cachedIds;
	}

	@Override
	public Object caseRunnableQuery(RunnableQuery object)
	{
	    try {
	        Variable variable = geppettoModelAccess.getPointer(object.getTargetVariablePath()).getElements().get(0).getVariable();
	        Query query = geppettoModelAccess.getQuery(object.getQueryPath());
	        String key = getKey(query, variable);

	        System.out.println("Processing query with key: " + key);
	        
	        if(cachedResults.containsKey(key))
	        {
	            System.out.println("Cache HIT for query: " + key);
	            QueryResults cachedResult = EcoreUtil.copy(cachedResults.get(key));
	            results.put(cachedResult, object.getBooleanOperator());
	            ids.put(cachedResult, new ArrayList<String>());
	            ids.get(cachedResult).addAll(cachedIds.get(key));
	            System.out.println("Retrieved " + (cachedIds.get(key) != null ? cachedIds.get(key).size() : "null") + " cached IDs");
	        }
	        else
	        {
	            try {
					System.out.println("Cache MISS for query: " + key);
	            	ExecuteQueryVisitor executeQueryVisitor = new ExecuteQueryVisitor(variable, geppettoModelAccess);
	            	executeQueryVisitor.doSwitch(query);
	            
	                List<String> resultIds = getIDs(executeQueryVisitor.getResults());
	                System.out.println("Query execution complete. Results size: " + 
	                    (executeQueryVisitor.getResults() != null ? executeQueryVisitor.getResults().getResults().size() : "null") + 
	                    ", IDs extracted: " + (resultIds != null ? resultIds.size() : "null"));
	                
	                try {
	                    cache(key, EcoreUtil.copy(executeQueryVisitor.getResults()), resultIds);
	                    System.out.println("Successfully cached results for key: " + key);
	                } catch (Exception e) {
	                    System.out.println("ERROR caching results: " + e.getMessage());
	                    e.printStackTrace();
	                }
	                
	                results.put(executeQueryVisitor.getResults(), object.getBooleanOperator());
	                ids.put(executeQueryVisitor.getResults(), new ArrayList<String>());
	                ids.get(executeQueryVisitor.getResults()).addAll(resultIds);
	            } catch (Exception e) {
	                System.out.println("ERROR processing query results: " + e.getMessage());
	                e.printStackTrace();
	            }
	        }
	    }
	    catch(GeppettoModelException e) {  // Removed GeppettoDataSourceException
	        System.out.println("ERROR in caseRunnableQuery: " + e.getMessage());
	        e.printStackTrace();
	        return new GeppettoVisitingException(e);
	    }

	    return super.caseRunnableQuery(object);
	}

	/**
	 * @param results
	 * @return
	 * @throws GeppettoDataSourceException
	 */
	private List<String> getIDs(QueryResults results) throws GeppettoDataSourceException
	{
	    List<String> resultsIDs = new ArrayList<String>();
	    
	    if (results == null) {
	        System.out.println("WARNING: QueryResults is null in getIDs()");
	        return resultsIDs;
	    }
	    
	    if (results.getHeader() == null) {
	        System.out.println("WARNING: Header is null in QueryResults");
	        return resultsIDs;
	    }
	    
	    System.out.println("Headers in results: " + String.join(", ", results.getHeader()));
	    
	    if(!results.getHeader().contains(ID))
	    {
	        System.out.println("WARNING: ID field not found in query results headers");
	        return resultsIDs;
	    }

	    try {
	        int baseId = results.getHeader().indexOf(ID);
	        System.out.println("ID column index: " + baseId);
	        
	        for(AQueryResult result : results.getResults())
	        {
	            if (result instanceof SerializableQueryResult) {
	                String id = ((SerializableQueryResult) result).getValues().get(baseId);
	                resultsIDs.add(id);
	            } else {
	                System.out.println("WARNING: Result is not a SerializableQueryResult: " + 
	                    (result != null ? result.getClass().getName() : "null"));
	            }
	        }
	        
	        System.out.println("Extracted " + resultsIDs.size() + " IDs from results");
	    } catch (Exception e) {
	        System.out.println("ERROR extracting IDs: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return resultsIDs;
	}

	/**
	 * @return
	 * @throws GeppettoDataSourceException
	 */
	public QueryResults getResults() throws GeppettoDataSourceException
	{

		if(results.keySet().size() > 1)
		{
			boolean first = true;
			for(QueryResults result : results.keySet())
			{
				if(finalResults.getHeader().isEmpty())
				{
					finalResults.getHeader().addAll(result.getHeader());
				}
				else
				{
					if(!finalResults.getHeader().equals(result.getHeader()))
					{
						throw new GeppettoDataSourceException("Multiple queries were executed but they returned incompatible headers");
					}
				}
				BooleanOperator o = results.get(result);
				switch(o)
				{
					case AND:
						if(first)
						{
							for(String id : ids.get(result))
							{

								finalResults.getResults().add(result.getResults().get(0)); // Note this will move the element from one list to another (EMF implementation) so although we always access
																							// the 0th element it's always a different one
								finalIds.add(id);

							}
						}
						List<String> toRemove = new ArrayList<String>();
						for(String id : finalIds)
						{
							if(!ids.get(result).contains(id)) 
							{
								toRemove.add(id);
							}
						}
						for(String id : toRemove)
						{
							finalResults.getResults().remove(finalIds.indexOf(id));
							finalIds.remove(id);
						}
						break;
					case OR:
						for(String id : ids.get(result))
						{
							if(!finalIds.contains(id))
							{
								finalResults.getResults().add(result.getResults().get(0)); // Note this will move the element from one list to another (EMF implementation) so although we always access
																							// the 0th element it's always a different one
								finalIds.add(id);
							}
						}
						break;
					case NAND:
						for(String id : ids.get(result))
						{
							if(finalIds.contains(id))
							{
								finalResults.getResults().remove(finalIds.indexOf(id));
								finalIds.remove(id);
							}
						}
						break;
				}
				first = false;
			}
		}
		else
		{
			// Just one element
			for(QueryResults result : results.keySet())
			{
				finalResults.getHeader().addAll(result.getHeader());
				finalResults = result;
			}
		}
		return finalResults;
	}

	/**
	 * @param key
	 * @param results
	 */
	private void cache(String key, QueryResults results, List<String> ids)
	{
	    System.out.println("Caching results for key: " + key);
	    System.out.println("Current cache size: " + cachedResults.size());
	    
	    if(cachedResults.size() > 10)
	    {
	        String removedKey = cachedResults.keySet().iterator().next();
	        System.out.println("Cache full, removing oldest entry: " + removedKey);
	        cachedResults.remove(removedKey);
	        cachedIds.remove(removedKey);
	    }
	    
	    try {
	        cachedResults.put(key, results);
	        cachedIds.put(key, new ArrayList<String>());
	        
	        if(ids != null) {
	            System.out.println("Adding " + ids.size() + " IDs to cache");
	            cachedIds.get(key).addAll(ids);
	        } else {
	            System.out.println("WARNING: IDs list is null");
	        }
	        
	        System.out.println("Cache operation complete. New cache size: " + cachedResults.size());
	    } catch (Exception e) {
	        System.out.println("ERROR during cache operation: " + e.getMessage());
	        e.printStackTrace();
	    }
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

	/**
	 * @throws GeppettoDataSourceException
	 * 
	 */
	public int getCount() throws GeppettoDataSourceException
	{
		return getResults().getResults().size();
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
	    this.page = page;
	}

}
