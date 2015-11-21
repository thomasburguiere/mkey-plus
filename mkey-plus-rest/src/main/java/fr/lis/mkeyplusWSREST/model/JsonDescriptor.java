package fr.lis.mkeyplusWSREST.model;

import fr.lis.xper3API.model.CategoricalDescriptor;
import fr.lis.xper3API.model.Descriptor;
import fr.lis.xper3API.model.QuantitativeDescriptor;

@SuppressWarnings("CallToSimpleSetterFromWithinClass")
public class JsonDescriptor {
	private String name;
	private String detail;
	private long[] stateIds;
	private long[] inapplicableState = null;
	private String unit;

	private long[] resourceIds;
	private boolean categoricalType = false;
	private boolean quantitativeType = false;
	private boolean calculatedType = false;
	private long id;

	public JsonDescriptor(Descriptor descriptor) {
		this.name = descriptor.getName();
		this.id = descriptor.getId();
		this.detail = descriptor.getDetail();
		this.resourceIds = new long[descriptor.getResources().size()];
		//Set resources
		for (int i = 0; i < descriptor.getResources().size(); i++) {
			resourceIds[i] = descriptor.getResources().get(i).getId();
		}
		if (descriptor.isCategoricalType()) {
			//Set States
			stateIds = new long[((CategoricalDescriptor) descriptor).getStates().size()];
			for (int j = 0; j < ((CategoricalDescriptor) descriptor).getStates().size(); j++) {
				stateIds[j] = ((CategoricalDescriptor) descriptor).getStates().get(j).getId();
			}
			this.categoricalType = true;
		}
		else if ( descriptor.isQuantitativeType() ){
			this.unit = ((QuantitativeDescriptor)descriptor).getMeasurementUnit();
			this.quantitativeType = true;
		}
		else if ( descriptor.isCalculatedType() ){
			this.calculatedType = true;
		}
	}

	public boolean isCalculatedType(){
		return calculatedType;
	}

	public boolean isQuantitativeType() {
		return quantitativeType;
	}

	public void setCalculatedType(boolean isCalculatedType ){
		this.calculatedType = isCalculatedType;
	}

	public void setQuantitativeType(boolean isQuantitativeType) {
		this.quantitativeType = isQuantitativeType;
	}

	public long getId() {
		return id;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public long[] getResourceIds() {
		return resourceIds;
	}

	public void setResourceIds(long[] resourceIds) {
		this.resourceIds = resourceIds;
	}

	public long[] getStateIds() {
		return stateIds;
	}

	public void setStateIds(long[] stateIds) {
		this.stateIds = stateIds;
	}

	public boolean isCategoricalType() {
		return categoricalType;
	}

	public void setCategoricalType(boolean isCategoricalType) {
		this.categoricalType = isCategoricalType;
	}

	public long[] getInapplicableState() {
		return inapplicableState;
	}

	public void setInapplicableState(long[] inapplicableState) {
		this.inapplicableState = inapplicableState;
	}
}
