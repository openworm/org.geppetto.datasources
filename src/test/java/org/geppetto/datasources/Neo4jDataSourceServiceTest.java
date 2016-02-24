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

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.geppetto.core.common.GeppettoHTTPClient;
import org.geppetto.core.common.JSONUtility;
import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.datasources.utils.VelocityUtils;
import org.geppetto.model.GeppettoFactory;
import org.geppetto.model.SimpleQuery;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author matteocantarelli
 *
 */
public class Neo4jDataSourceServiceTest
{

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
	}

	/**
	 * Test method for {@link org.geppetto.datasources.Neo4jDataSourceService#getNumberOfResults(org.geppetto.model.Query, org.geppetto.model.variables.Variable)}.
	 */
	@Test
	public void testGetNumberOfResultsQueryVariable()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.geppetto.datasources.Neo4jDataSourceService#getNumberOfResults(org.geppetto.model.Query, org.geppetto.model.variables.Variable, org.geppetto.model.QueryResults)}.
	 */
	@Test
	public void testGetNumberOfResultsQueryVariableQueryResults()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.geppetto.datasources.Neo4jDataSourceService#execute(org.geppetto.model.Query, org.geppetto.model.variables.Variable, org.geppetto.core.datasources.IQueryListener)}.
	 */
	@Test
	public void testExecuteQueryVariableIQueryListener()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.geppetto.datasources.Neo4jDataSourceService#execute(org.geppetto.model.Query, org.geppetto.model.variables.Variable, org.geppetto.model.QueryResults, org.geppetto.core.datasources.IQueryListener)}
	 * .
	 */
	@Test
	public void testExecuteQueryVariableQueryResultsIQueryListener()
	{
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.geppetto.datasources.Neo4jDataSourceService#fetchVariable(java.lang.String)}.
	 * 
	 * @throws GeppettoDataSourceException
	 */
	@Test
	public void testFetchVariable() throws GeppettoDataSourceException
	{
		Neo4jDataSourceService dataSource = new Neo4jDataSourceService();

		SimpleQuery query = GeppettoFactory.eINSTANCE.createSimpleQuery();
		//Bad query //TODO HANDLE
		//query.setQuery("MATCH (n:Class) WHERE n.short_form='$ID' RETURN n.label, n.short_form, n.description, n.comment LIMIT 1;");
		query.setQuery("MATCH (n:VFB:Class { short_form: '$ID' } ) RETURN n.label, n.short_form, n.description, n.description LIMIT 1;");
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("ID", "FBbt_00100219"); //will be coming from the server
		properties.put("QUERY", query);
		String queryString = VelocityUtils.processTemplate("/templates/neo4j/queryTemplate.vm", properties);
		String response = GeppettoHTTPClient.doJSONPost("http://vfbdev.inf.ed.ac.uk/neo4jdb/data/transaction", queryString);

		Map<String, Object> responseMap = JSONUtility.getAsMap(response);
		
		System.out.println(response);
	}

}
