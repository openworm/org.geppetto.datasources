package org.geppetto.datasources.vfbquery;

import org.geppetto.datasources.ADataSourceService;
import org.geppetto.datasources.IQueryResponseProcessor;

/**
 * GET-based datasource for the VFBquery {@code get_term_info} endpoint on
 * v3-cached.virtualflybrain.org. Issues {@code GET <url>?id=<short_form>} and
 * hands the raw term-info JSON to {@link VFBqueryTermInfoResponseProcessor}
 * (which exposes it as a single {@code term_info} value). Used by the term-info
 * fetchVariableQuery, processed by
 * uk.ac.vfb.geppetto.VFBProcessTermInfoVFBqueryJson.
 *
 * Separate from {@link VFBqueryDataSourceService} because that one's response
 * processor only understands the {@code {headers, rows}} run_query envelope.
 *
 * @author robertcourt
 */
public class VFBqueryTermInfoDataSourceService extends ADataSourceService
{

	public VFBqueryTermInfoDataSourceService()
	{
		super("/templates/vfbquery/queryTemplate.vm");
	}

	@Override
	public ConnectionType getConnectionType()
	{
		return ConnectionType.GET;
	}

	@Override
	public IQueryResponseProcessor getQueryResponseProcessor()
	{
		if(queryResponseProcessor == null)
		{
			queryResponseProcessor = new VFBqueryTermInfoResponseProcessor();
		}
		return queryResponseProcessor;
	}

}
