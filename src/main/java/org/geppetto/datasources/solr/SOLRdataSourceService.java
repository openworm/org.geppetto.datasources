package org.geppetto.datasources.solr;

import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.datasources.IQueryListener;
import org.geppetto.datasources.ADataSourceService;
import org.geppetto.datasources.ExecuteQueryVisitor;
import org.geppetto.datasources.IQueryResponseProcessor;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;

/**
 * @author matteocantarelli
 *
 */
public class SOLRdataSourceService extends ADataSourceService
{
    public SOLRdataSourceService()
    {
        super("/templates/SOLR/queryTemplate.vm");
        System.out.println("SOLR Data Source Service created");
    }
    
    @Override
    public ConnectionType getConnectionType()
    {
        return ConnectionType.POST;
    }

    @Override
    public IQueryResponseProcessor getQueryResponseProcessor()
    {
        if(queryResponseProcessor == null)
        {
            queryResponseProcessor = new SOLRresponseProcessor();
        }
        return queryResponseProcessor;
    }
    
    // Helper method to debug SOLR queries
    public void debugQuery(String query, String url) {
        System.out.println("SOLR Query to " + url + ":");
        System.out.println(query);
    }
}
