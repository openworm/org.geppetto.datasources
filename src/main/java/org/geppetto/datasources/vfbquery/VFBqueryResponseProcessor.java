/**
 *
 */
package org.geppetto.datasources.vfbquery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.DatasourcesFactory;
import org.geppetto.model.datasources.QueryResult;
import org.geppetto.model.datasources.QueryResults;

/**
 * Unwraps the canonical VFBquery response envelope into Geppetto QueryResults.
 *
 * Input shape (from /run_query or the per-query entries of /get_term_info):
 * {
 *   "headers": {
 *     "<col_id>": {"title": "...", "type": "selection_id|markdown|number|tags|list|metadata|...",
 *                  "order": <int>, "sort": {...}? },
 *     ...
 *   },
 *   "rows": [ {"<col_id>": <value>, ...}, ... ],
 *   "count": <int>   // optional
 * }
 *
 * Output:
 *   - header  = column titles ordered by the header entry's `order` field
 *               (selection_id, conventionally `order=-1`, ends up first as "ID").
 *   - results = one QueryResult per row, values in the same column order.
 *
 * Cell values are passed through as java.lang.Object so downstream processors
 * (e.g. uk.ac.vfb.geppetto.VFBqueryJsonProcessor) can format them per column
 * type and add VFB-specific shape work (synthesised queried-term columns for
 * connectivity tables, etc.).
 *
 * @author robertcourt
 */
public class VFBqueryResponseProcessor implements IQueryResponseProcessor
{

	// org.geppetto.datasources bundle is NOT touched by the geppetto-vfb dev
	// build's `sed s@Boolean debug=...@true@` step (the sed only walks
	// uk.ac.vfb.geppetto/). For early-migration diagnostics this is wired on
	// permanently; flip back to false once Shape-A queries are confirmed
	// rendering correctly on v2-dev.
	private static final boolean DEBUG = true;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.geppetto.datasources.IQueryResponseProcessor#processResponse(java.util.Map)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public QueryResults processResponse(Map<String, Object> response)
	{
		QueryResults results = DatasourcesFactory.eINSTANCE.createQueryResults();

		if(response == null || !response.containsKey("headers") || !response.containsKey("rows"))
		{
			// Empty / malformed response — return an empty QueryResults so the
			// chain continues without throwing. Downstream processors can detect
			// the empty case via results.getResults().isEmpty().
			if(DEBUG)
			{
				System.out.println("VFBqueryResponseProcessor: malformed response (null/missing keys). Top-level keys: "
						+ (response == null ? "<null>" : response.keySet()));
			}
			return results;
		}

		Map<String, Map<String, Object>> headers = (Map<String, Map<String, Object>>) response.get("headers");
		List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");

		if(DEBUG)
		{
			System.out.println("VFBqueryResponseProcessor: headers type=" + (headers == null ? "<null>" : headers.getClass().getName())
					+ ", header keys=" + (headers == null ? "<null>" : headers.keySet())
					+ ", rows type=" + (rows == null ? "<null>" : rows.getClass().getName())
					+ ", rows.size=" + (rows == null ? -1 : rows.size())
					+ ", response.count=" + response.get("count"));
		}

		// Sort columns by `order` (selection_id is conventionally -1 so it lands first).
		// Stable for ties; missing order treated as Integer.MAX_VALUE so unspecified columns sink to the end.
		List<Map.Entry<String, Map<String, Object>>> orderedColumns = new ArrayList<>(headers.entrySet());
		orderedColumns.sort(new Comparator<Map.Entry<String, Map<String, Object>>>()
		{
			@Override
			public int compare(Map.Entry<String, Map<String, Object>> a, Map.Entry<String, Map<String, Object>> b)
			{
				return Integer.compare(orderOf(a.getValue()), orderOf(b.getValue()));
			}
		});

		// Preserve the column-id → title mapping so downstream processors can introspect.
		// We add titles to the header in column order, but also stash the raw column id
		// as the row value key so a downstream processor can re-derive the per-column type.
		Map<String, String> idToTitle = new LinkedHashMap<>();
		for(Map.Entry<String, Map<String, Object>> col : orderedColumns)
		{
			String colId = col.getKey();
			Object title = col.getValue().get("title");
			String headerLabel = title != null ? title.toString() : colId;
			results.getHeader().add(headerLabel);
			idToTitle.put(colId, headerLabel);
		}

		// Walk rows in the same column order; preserve Object types so number/list/etc.
		// can be formatted further downstream.
		if(rows != null)
		{
			for(Map<String, Object> rowObject : rows)
			{
				QueryResult resultRow = DatasourcesFactory.eINSTANCE.createQueryResult();
				for(String colId : idToTitle.keySet())
				{
					Object v = rowObject.get(colId);
					resultRow.getValues().add(v);
				}
				results.getResults().add(resultRow);
			}
		}

		if(DEBUG)
		{
			// Cast to the concrete QueryResult (the only subclass we emit here)
			// so getValues() resolves — AQueryResult is abstract and does not
			// expose it. Same EClass pitfall that originally broke
			// VFBqueryJsonProcessor; do NOT re-introduce on AQueryResult here.
			String firstRow = "";
			if(!results.getResults().isEmpty())
			{
				firstRow = " firstRowValues=" + ((QueryResult) results.getResults().get(0)).getValues();
			}
			System.out.println("VFBqueryResponseProcessor: built QueryResults"
					+ " header=" + results.getHeader()
					+ " resultsRows=" + results.getResults().size()
					+ firstRow);
		}

		return results;
	}

	private static int orderOf(Map<String, Object> col)
	{
		Object o = col.get("order");
		if(o instanceof Number)
		{
			return ((Number) o).intValue();
		}
		return Integer.MAX_VALUE;
	}

}
