package Sudden_Stop;

import static repast.simphony.random.RandomHelper.*;
import static repast.simphony.essentials.RepastEssentials.*;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;

public class ModelLoader extends DefaultContext<Object> implements
		ContextBuilder<Object> {

	@Override
	public Context<Object> build(Context<Object> context) {

		/*
		 * Sets the Random Helper Seed
		 */
		Integer seed = (Integer) GetParameter("randomSeed");
		if (seed != null)
			setSeed(seed);

		context.setId("Sudden_Stop");

		return context;
		
	}
	
	

}
