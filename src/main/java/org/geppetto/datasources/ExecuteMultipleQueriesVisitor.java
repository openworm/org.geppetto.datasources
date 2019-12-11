
package org.geppetto.datasources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
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

	public ExecuteMultipleQueriesVisitor(GeppettoModelAccess geppettoModelAccess, Map<String, QueryResults> cachedResults, Map<String, List<String>> cachedIds)
	{
		this.geppettoModelAccess = geppettoModelAccess;
		this.cachedResults = cachedResults;
		this.cachedIds = cachedIds;
	}

	@Override
	public Object caseRunnableQuery(RunnableQuery object)
	{
		try
		{

			Variable variable = geppettoModelAccess.getPointer(object.getTargetVariablePath()).getElements().get(0).getVariable();
			Query query = geppettoModelAccess.getQuery(object.getQueryPath());
			String key = getKey(query, variable);

			if(cachedResults.containsKey(key))
			{
				QueryResults cachedResult = EcoreUtil.copy(cachedResults.get(key));
				results.put(cachedResult, object.getBooleanOperator());
				ids.put(cachedResult, new ArrayList<String>());
				ids.get(cachedResult).addAll(cachedIds.get(key));
			}
			else
			{
				ExecuteQueryVisitor executeQueryVisitor = new ExecuteQueryVisitor(variable, geppettoModelAccess);
				executeQueryVisitor.doSwitch(query);
				List<String> resultIds = getIDs(executeQueryVisitor.getResults());
				cache(key, EcoreUtil.copy(executeQueryVisitor.getResults()), resultIds);
				results.put(executeQueryVisitor.getResults(), object.getBooleanOperator());
				ids.put(executeQueryVisitor.getResults(), new ArrayList<String>());
				ids.get(executeQueryVisitor.getResults()).addAll(resultIds);
			}

		}
		catch(GeppettoModelException | GeppettoDataSourceException e)
		{
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
		if(!results.getHeader().contains(ID))
		{
			throw new GeppettoDataSourceException("The queries don't have an ID field");
		}

		int baseId = results.getHeader().indexOf(ID);

		for(AQueryResult result : results.getResults())
		{
			resultsIDs.add(((SerializableQueryResult) result).getValues().get(baseId));
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
				// If no ID present in one of the results, return '0 results'
				if(!result.getHeader().contains(ID)){
					finalResults = DatasourcesFactory.eINSTANCE.createQueryResults();
					return finalResults;
				}
				// If it's the first result, add the headers to 'finalResults' before continuing
				if(first){
					finalResults.getHeader().addAll(result.getHeader());
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
							// Merge results, only for second result and beyond, and if ID in new results matches
							// one in the first result.
							else if(ids.get(result).contains(id) && !first){
								mergeResult(result, id);
							}
						}
						for(String id : toRemove)
						{
							finalResults.getResults().remove(finalIds.indexOf(id));
							finalIds.remove(id);
						}
						
						// Add headers for new result to 'finalResults' list, this is done after merging results 
						// to avoid having a mismatch of values and headers prior to the merge.
						if(!first){
							finalResults.getHeader().addAll(result.getHeader());
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
		if(cachedResults.size() > 10)
		{
			cachedResults.remove(cachedResults.keySet().iterator().next());
			cachedIds.remove(cachedIds.keySet().iterator().next());
		}
		cachedResults.put(key, results);
		cachedIds.put(key, new ArrayList<String>());
		cachedIds.get(key).addAll(ids);

	}
	
	/**
	 * This function takes new QueryResults, 'result' parameter, and merges it with an existing QueryResult in 
	 * 'finalResults' list. The match happens using the 'id' parameter, if no match is present nothing is done.
	 * 
	 * @param result - The new result to be merged into 'finalResults'.
	 * @param id - The id of the existing result found in 'finalResults' that matches the new result
	 */
	private void mergeResult(QueryResults result, String id){
		// Extract index from 'result' list
		int resultIndex = ids.get(result).indexOf(id);
		// Extract the ID column value from 'result' that matched the passed 'id'
		String resultID = 
				((SerializableQueryResult) result.getResults().get(resultIndex)).getValues().get(result.getHeader().indexOf(ID));
		// Extract the existing result in 'finalResults' that matches the one in the second result
		SerializableQueryResult existingResult = 
				(SerializableQueryResult) finalResults.getResults().get(finalIds.indexOf(resultID));
		// Extract column headers for new result
		EList<String> newResultHeaders = result.getHeader();
		// Extract column headers from 'finalResults', belonging to previous result(s)
		EList<String> finalResultsHeaders = finalResults.getHeader();
		// Extract column values for new result
		EList<String> resultValues = ((SerializableQueryResult) result.getResults().get(resultIndex)).getValues();
		// Loop through headers of new result, and if a header is not present in 'finalResults' add it
		// to the matching existing result in that list.
		for(String header : newResultHeaders){
			int indexFinalHeaders = finalResultsHeaders.indexOf(header);
			int newResultIndexHeader = newResultHeaders.indexOf(header);
			// If header is not present in result found in 'finalResults' list, we add the new value to the result
			if(indexFinalHeaders==-1){
				existingResult.getValues().add(resultValues.get(newResultIndexHeader));
			}
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

}
