package fr.lis.mkeyplusWSREST.model;

import fr.lis.xper3API.model.State;

public class JsonState {
	private String name;
	private String detail;
	private long[] resourceIds;
	private long id;
	
	public long getId() {
		return id;
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

	public long[] getResourceIds() {
		return resourceIds;
	}

	public void setResourceIds(long[] resourceIds) {
		this.resourceIds = resourceIds;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public JsonState(State state) {
		this.name = state.getName();
		this.detail = state.getDetail();
		this.id = state.getId();
		this.resourceIds = new long[state.getResources().size()];
		for (int i = 0; i < state.getResources().size(); i++) {
			this.resourceIds[i] = state.getResources().get(i).getId();
		}
	}
}