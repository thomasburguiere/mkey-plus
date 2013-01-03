package fr.lis.mkeyplusAPI.model;

import java.util.List;

import model.CategoricalDescriptor;
import model.Descriptor;
import model.State;

public class ExtCategoricalDescriptor extends CategoricalDescriptor {
	private Descriptor parentDescriptor;
	private List<State> inapplicableStates;

	public List<State> getInapplicableStates() {
		return inapplicableStates;
	}

	public void setInapplicableStates(List<State> inapplicableStates) {
		this.inapplicableStates = inapplicableStates;
	}

	public Descriptor getParentDescriptor() {
		return parentDescriptor;
	}

	public void setParentDescriptor(Descriptor parentDescriptor) {
		this.parentDescriptor = parentDescriptor;
	}

}
