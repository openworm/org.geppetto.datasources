
package org.geppetto.datasources;

import java.util.Map;

import org.geppetto.model.datasources.QueryResults;

/**
 * @author matteocantarelli
 *
 */
public interface IQueryResponseProcessor
{

	QueryResults processResponse(Map<String, Object> response);

}
