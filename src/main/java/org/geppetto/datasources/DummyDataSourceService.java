
package org.geppetto.datasources;

import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.datasources.neo4j.Neo4jResponseProcessor;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.datasources.DataSource;
import org.geppetto.model.datasources.DataSourceLibraryConfiguration;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.TypesFactory;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;

/**
 * @author matteocantarelli
 *
 */
public class DummyDataSourceService extends ADataSourceService
{

	public DummyDataSourceService()
	{
		super("");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.IDataSource#fetchVariable(java.lang.String)
	 */
	@Override
	public void fetchVariable(String variableId) throws GeppettoDataSourceException
	{
		Variable fetchedVariable = VariablesFactory.eINSTANCE.createVariable();
		fetchedVariable.setId(variableId);
		getGeppettoModelAccess().addVariable(fetchedVariable);
		ImportType importType = TypesFactory.eINSTANCE.createImportType();
		importType.setId("Type" + variableId); // an SWC for instance
		importType.setUrl(""); // an SWC for instance
		fetchedVariable.getTypes().add(importType);
		importType.setModelInterpreterId("swcModelInterpreter");
		getGeppettoModelAccess().addTypeToLibrary(importType, getLibraryFor(getConfiguration(), "swc"));

	}

	/**
	 * @param dataSource
	 * @param format
	 * @return
	 */
	private GeppettoLibrary getLibraryFor(DataSource dataSource, String format)
	{
		for(DataSourceLibraryConfiguration lc : dataSource.getLibraryConfigurations())
		{
			if(lc.getFormat().equals(format))
			{
				return lc.getLibrary();
			}
		}
		return null;
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
			queryResponseProcessor = new Neo4jResponseProcessor();
		}
		return queryResponseProcessor;
	}

}
