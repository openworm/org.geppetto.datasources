/**
 * 
 */
package org.geppetto.datasources;

import java.util.HashMap;
import java.util.Map;

import org.geppetto.core.datasources.IQueryProcessor;
import org.geppetto.core.features.IFeature;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.services.registry.ServicesRegistry;

/**
 * @author matteocantarelli
 *
 */
public abstract class AQueryProcessor implements IQueryProcessor
{

	private Map<String, Object> outputMap = new HashMap<>();

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.datasources.IQueryProcessor#getProcessingOutputMap()
	 */
	@Override
	public Map<String, Object> getProcessingOutputMap()
	{
		if (debug) {
            System.out.println("Processing output map contents from " + this.getClass().getName());
            for (Map.Entry<String, Object> entry : processingOutputMap.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
        }
		return outputMap;
	}

}
