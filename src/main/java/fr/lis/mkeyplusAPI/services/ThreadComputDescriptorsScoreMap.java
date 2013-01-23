package fr.lis.mkeyplusAPI.services;

import java.util.List;
import java.util.concurrent.Callable;

import model.CategoricalDescriptor;
import model.DescriptionElementState;
import model.Descriptor;
import model.DescriptorTree;
import model.Item;

public class ThreadComputDescriptorsScoreMap implements Callable<Object[]> {

	private Descriptor descriptor = null;
	private float discriminantPower = -1;
	private List<Item> items = null;
	private DescriptorTree dependencyTree = null;
	private boolean considerChildScores = true;
	private int scoreMethod = -1;
	private DescriptionElementState[][] descriptionMatrix;

	public ThreadComputDescriptorsScoreMap(List<Item> items, DescriptorTree dependencyTree, int scoreMethod,
			boolean considerChildScores, Descriptor descriptor, DescriptionElementState[][] descriptionMatrix) {
		this.descriptor = descriptor;
		this.items = items;
		this.dependencyTree = dependencyTree;
		this.considerChildScores = considerChildScores;
		this.scoreMethod = scoreMethod;
		this.descriptionMatrix = descriptionMatrix;
	}

	public Object[] call() {
		Object[] output = new Object[2];

		if (descriptor.isCategoricalType() && ((CategoricalDescriptor) descriptor).getStates().size() <= 0) {
			discriminantPower = 0;
		}

		else {
			discriminantPower = InteractiveIdentificationService.getDiscriminantPower(descriptor, items, 0,
					scoreMethod, considerChildScores, dependencyTree, descriptionMatrix);

		}

		output[0] = descriptor;
		output[1] = discriminantPower;
		return output;
	}

}
