
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
				// Merge all headers into finalResults instead of only when is empty
				finalResults.getHeader().addAll(result.getHeader());
				
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
