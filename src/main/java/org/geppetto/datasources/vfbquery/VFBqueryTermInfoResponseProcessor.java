package org.geppetto.datasources.vfbquery;

import java.util.Map;

import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;

import com.google.gson.Gson;

/**
 * Response processor for the VFBquery {@code get_term_info} endpoint.
 *
 * Unlike {@link VFBqueryResponseProcessor} (which unwraps the
 * {@code {headers, rows}} run_query envelope into a results table), the
 * {@code get_term_info} response is a single term-info display object
 * ({@code Id}, {@code Meta}, {@code Synonyms}, {@code Queries[]}, ...). The
 * downstream Java processor (uk.ac.vfb.geppetto.VFBProcessTermInfoVFBqueryJson)
 * expects the whole JSON as one {@code term_info} value — mirroring how the SOLR
 * term-info datasource exposes its {@code term_info} field — so this processor
 * re-serialises the parsed response into a single-cell QueryResults under the
 * header {@code term_info}.
 *
 * @author robertcourt
 */
public class VFBqueryTermInfoResponseProcessor implements IQueryResponseProcessor
{

	@Override
	public QueryResults processResponse(Map<String, Object> response)
	{
		QueryResults results = DatasourcesFactory.eINSTANCE.createQueryResults();
		results.getHeader().add("term_info");

		QueryResult row = DatasourcesFactory.eINSTANCE.createQueryResult();
		String json = (response == null) ? "" : new Gson().toJson(response);
		row.getValues().add(json);
		results.getResults().add(row);

		return results;
	}

}
