package org.geppetto.datasources;



import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.features.IFeature;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.model.datasources.DataSource;
import org.geppetto.model.datasources.ProcessQuery;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.types.CompositeType;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.TypesFactory;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;

/**
 * @author matteocantarelli
 *
 */
public class TestQueryProcessor extends AQueryProcessor
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.datasources.IQueryProcessor#process(org.geppetto.model.ProcessQuery, org.geppetto.model.variables.Variable, org.geppetto.model.QueryResults)
	 */
	@Override
	public QueryResults process(ProcessQuery query, DataSource dataSource, Variable variable, QueryResults results, GeppettoModelAccess geppettoModelAccess) throws GeppettoDataSourceException
	{
		CompositeType type = TypesFactory.eINSTANCE.createCompositeType();
		type.setId(variable.getId());
		variable.getAnonymousTypes().add(type);
		
		Variable importTypeVar = VariablesFactory.eINSTANCE.createVariable();
		importTypeVar.setId("testImportVar");
		importTypeVar.setName("testImportVar");
		
		ImportType importType=TypesFactory.eINSTANCE.createImportType();
		importType.setId("testImportType");
		importType.setName("testImportType");
		importType.setModelInterpreterId("testModelInterpreter");
		importType.setUrl("http://geppetto.org");
		
		geppettoModelAccess.addTypeToLibrary(importType, dataSource.getDependenciesLibrary().get(0));
		
		importTypeVar.getTypes().add(importType);
		type.getVariables().add(importTypeVar);

		return results;
	}

	@Override
	public void registerGeppettoService() throws Exception
	{
		ServicesRegistry.registerQueryProcessorService(this);
	}

	@Override
	public boolean isSupported(GeppettoFeature feature)
	{
		return false;
	}

	@Override
	public IFeature getFeature(GeppettoFeature feature)
	{
		return null;
	}

	@Override
	public void addFeature(IFeature feature)
	{

	}

}
