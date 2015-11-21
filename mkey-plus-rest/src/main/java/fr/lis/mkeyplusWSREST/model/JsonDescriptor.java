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
	private boolean isCategoricalType = false;
	private boolean isQuantitativeType = false;
	private boolean isCalculatedType = false;
	private long id;

	public JsonDescriptor(Descriptor descriptor) {
		this.name = descriptor.getName();
		this.id = descriptor.getId();
		this.detail = descriptor.getDetail();
		this.resourceIds = new long[descriptor.getResources().size()];
		//Set resources
		for (int i = 0; i < descriptor.getResources().size(); i++) {
			this.resourceIds[i] = descriptor.getResources().get(i).getId();
		}
		if (descriptor.isCategoricalType()) {
			//Set States
			stateIds = new long[((CategoricalDescriptor) descriptor).getStates().size()];
			for (int j = 0; j < ((CategoricalDescriptor) descriptor).getStates().size(); j++) {
				stateIds[j] = ((CategoricalDescriptor) descriptor).getStates().get(j).getId();
			}
			this.isCategoricalType = true;
		}
		else if ( descriptor.isQuantitativeType() ){
			this.unit = ((QuantitativeDescriptor)descriptor).getMeasurementUnit();
			this.isQuantitativeType = true;
		}
		else if ( descriptor.isCalculatedType() ){
			this.isCalculatedType = true;
		}
	}

	public boolean isCalculatedType(){
		return isCalculatedType;
	}
	
	public boolean isQuantitativeType() {
		return isQuantitativeType;
	}

	public void setCalculatedType(boolean isCalculatedType ){
		this.isCalculatedType = isCalculatedType;
	}
	
	public void setQuantitativeType(boolean isQuantitativeType) {
		this.isQuantitativeType = isQuantitativeType;
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
		return isCategoricalType;
	}

	public void setCategoricalType(boolean isCategoricalType) {
		this.isCategoricalType = isCategoricalType;
	}

	public long[] getInapplicableState() {
		return inapplicableState;
	}

	public void setInapplicableState(long[] inapplicableState) {
		this.inapplicableState = inapplicableState;
	}
}
