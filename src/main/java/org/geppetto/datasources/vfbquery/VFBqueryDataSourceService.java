/**
 *
 */
package org.geppetto.datasources.vfbquery;

import org.geppetto.datasources.ADataSourceService;
import org.geppetto.datasources.IQueryResponseProcessor;

/**
 * GET-based datasource for the VFBquery cached API exposed by
 * v3-cached.virtualflybrain.org. Calls are issued as
 *
 *   GET <url>?id=<short_form>&query_type=<QueryName>
 *
 * matching the URL format used by the V3 frontend
 * (virtual-fly-brain/applications/.../frontend/src/network/query.js)
 * and VFB3-MCP (VFB3-MCP/src/index.ts) so all three clients share the
 * same nginx cache key on v3-cached.
 *
 * Response shape: {"headers": {...}, "rows": [...], "count": N}
 * unwrapped by VFBqueryResponseProcessor.
 *
 * @author robertcourt
 */
public class VFBqueryDataSourceService extends ADataSourceService
{

	public VFBqueryDataSourceService()
	{
		super("/templates/vfbquery/queryTemplate.vm");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.geppetto.datasources.ADataSourceService#getConnectionType()
	 */
	@Override
	public ConnectionType getConnectionType()
	{
		return ConnectionType.GET;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.geppetto.datasources.ADataSourceService#getQueryResponseProcessor()
	 */
	@Override
	public IQueryResponseProcessor getQueryResponseProcessor()
	{
		if(queryResponseProcessor == null)
		{
			queryResponseProcessor = new VFBqueryResponseProcessor();
		}
		return queryResponseProcessor;
	}

}
