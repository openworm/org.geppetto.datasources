
package org.geppetto.datasources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.common.GeppettoAccessException;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.DefaultGeppettoDataManager;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IUserGroup;
import org.geppetto.core.data.model.UserPrivileges;
import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.services.registry.ApplicationListenerBean;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoModel;
import org.geppetto.model.types.CompositeType;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.Type;
import org.geppetto.model.util.GeppettoModelException;
import org.geppetto.simulation.manager.ExperimentRunManager;
import org.geppetto.simulation.manager.GeppettoManager;
import org.geppetto.simulation.manager.RuntimeProject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * 
 * This is an integration test which checks the workkflows of the GeppettoManager. Provides coverage also for RuntimeProject and RuntimeExperiment.
 * 
 * @author matteocantarelli
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataSourcesGeppettoManagerTest
{

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private static GeppettoManager manager = new GeppettoManager(Scope.CONNECTION);
	private static IGeppettoProject geppettoProject;
	private static RuntimeProject runtimeProject;

	/**
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("deprecation")
	@BeforeClass
	public static void setUp() throws Exception
	{
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		BeanDefinition modelInterpreterBeanDefinition = new RootBeanDefinition(TestModelInterpreterService.class);
		BeanDefinition dataSourceBeanDefinition = new RootBeanDefinition(TestDataSourceService.class);
		BeanDefinition queryProcessorBeanDefinition = new RootBeanDefinition(TestQueryProcessor.class);
		context.registerBeanDefinition("testModelInterpreter", modelInterpreterBeanDefinition);
		context.registerBeanDefinition("scopedTarget.testModelInterpreter", modelInterpreterBeanDefinition);
		context.registerBeanDefinition("testDataSourceService", dataSourceBeanDefinition);
		context.registerBeanDefinition("scopedTarget.testDataSourceService", dataSourceBeanDefinition);
		context.registerBeanDefinition("testQueryProcessorService", queryProcessorBeanDefinition);
		context.registerBeanDefinition("scopedTarget.testQueryProcessorService", queryProcessorBeanDefinition);
		context.refresh();
		ContextRefreshedEvent event = new ContextRefreshedEvent(context);
		ApplicationListenerBean listener = new ApplicationListenerBean();
		listener.onApplicationEvent(event);
		ApplicationContext retrievedContext = ApplicationListenerBean.getApplicationContext("testModelInterpreter");
		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.testModelInterpreter"));
		Assert.assertTrue(retrievedContext.getBean("scopedTarget.testModelInterpreter") instanceof TestModelInterpreterService);
		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.testDataSourceService"));
		Assert.assertTrue(retrievedContext.getBean("scopedTarget.testDataSourceService") instanceof TestDataSourceService);
		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.testQueryProcessorService"));
		Assert.assertTrue(retrievedContext.getBean("scopedTarget.testQueryProcessorService") instanceof TestQueryProcessor);

		DataManagerHelper.setDataManager(new DefaultGeppettoDataManager());
		Assert.assertNotNull(ExperimentRunManager.getInstance());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#setUser(org.geppetto.core.data.model.IUser)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test01SetUser() throws GeppettoExecutionException
	{
		long value = 1000l * 1000 * 1000;
		List<UserPrivileges> privileges = new ArrayList<UserPrivileges>();
		privileges.add(UserPrivileges.READ_PROJECT);
		privileges.add(UserPrivileges.WRITE_PROJECT);
		privileges.add(UserPrivileges.DOWNLOAD);
		privileges.add(UserPrivileges.DROPBOX_INTEGRATION);
		privileges.add(UserPrivileges.RUN_EXPERIMENT);
		IUserGroup userGroup = DataManagerHelper.getDataManager().newUserGroup("unaccountableAristocrats", privileges, value, value * 2);
		manager.setUser(DataManagerHelper.getDataManager().newUser("nonna", "passauord", true, userGroup));
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#getUser()}.
	 */
	@Test
	public void test02GetUser()
	{
		Assert.assertEquals("nonna", manager.getUser().getName());
		Assert.assertEquals("passauord", manager.getUser().getPassword());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadProject(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws IOException
	 * @throws GeppettoExecutionException
	 * @throws GeppettoInitializationException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test03LoadProject() throws IOException, GeppettoInitializationException, GeppettoExecutionException, GeppettoAccessException
	{
		InputStreamReader inputStreamReader = new InputStreamReader(DataSourcesGeppettoManagerTest.class.getResourceAsStream("/test/geppettoManagerDataSourceTest.json"));
		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader, null);
		manager.loadProject("1", geppettoProject);

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#getRuntimeProject(org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test04RuntimeProject() throws GeppettoExecutionException
	{
		runtimeProject = manager.getRuntimeProject(geppettoProject);
		Assert.assertNotNull(runtimeProject);

		// Testing libraries are there
		GeppettoModel geppettoModel = runtimeProject.getGeppettoModel();
		Assert.assertEquals(2, geppettoModel.getLibraries().size());
		GeppettoLibrary common = geppettoModel.getLibraries().get(1);
		Assert.assertEquals("common", common.getId());
		Assert.assertEquals("Geppetto Common Library", common.getName());
		GeppettoLibrary testLibrary = geppettoModel.getLibraries().get(0);
		Assert.assertEquals("testLibrary", testLibrary.getId());
		Assert.assertEquals("testLibrary", testLibrary.getName());

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test05LoadExperiment() throws GeppettoExecutionException, GeppettoAccessException
	{
		ExperimentState experimentState = manager.loadExperiment("1", geppettoProject.getExperiments().get(0));
		Assert.assertNotNull(experimentState);

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 * @throws GeppettoModelException
	 * @throws GeppettoDataSourceException
	 */
	@Test
	public void test06FetchVariable() throws GeppettoExecutionException, GeppettoAccessException, GeppettoDataSourceException, GeppettoModelException
	{
		// the duplicates are intentional, the RunTimeProject class will check if duplicates want to be fetched or if they are already in the current model.
		String[] idsList = {"testVariable", "testVariable", "testVariable2", "testVariable2", "testVariable"};
		GeppettoModel geppettoModel = runtimeProject.getGeppettoModel();
		Assert.assertEquals(1, geppettoModel.getVariables().size()); // only "time" //FIXME Should time be there without a simulation?
		GeppettoModel model = manager.fetchVariable("testDataSource", idsList, geppettoProject);
		Assert.assertEquals("testVariable", model.getVariables().get(1).getId());
		Assert.assertEquals("testVariable2", model.getVariables().get(2).getId());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test07ResolveImportType() throws GeppettoExecutionException, GeppettoAccessException
	{
		GeppettoModel geppettoModel = runtimeProject.getGeppettoModel();
		Type type = geppettoModel.getLibraries().get(0).getTypes().get(0);
		Assert.assertEquals(2, geppettoModel.getLibraries().get(0).getTypes().size());
		Assert.assertEquals("testImportType", type.getId());
		Assert.assertTrue(type instanceof ImportType);
		List<String> types=new ArrayList<String>();
		types.add("testLibrary.testImportType");
		GeppettoModel model = manager.resolveImportType(types, geppettoProject);
		type = model.getLibraries().get(0).getTypes().get(1);
		Assert.assertTrue(type instanceof CompositeType);
		Assert.assertEquals(2, model.getLibraries().get(0).getTypes().size()); //still only one type but this time it's a composite
		Assert.assertEquals(5, ((CompositeType) type).getVariables().size());

	}


}
