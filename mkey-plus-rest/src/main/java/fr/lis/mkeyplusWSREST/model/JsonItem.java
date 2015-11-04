package fr.lis.mkeyplusWSREST.model;

import fr.lis.xper3API.model.Item;

public class JsonItem {
	private String name;
	private String alternativeName;
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

	public String getAlternativeName() {
		return alternativeName;
	}

	public void setAlternativeName(String alternativeName) {
		this.alternativeName = alternativeName;
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

	public JsonItem(Item item) {
		this.name = item.getName();
		this.alternativeName = item.getAlternativeName();
		this.detail = item.getDetail();
		this.id = item.getId();
		this.resourceIds = new long[item.getResources().size()];
		for (int i = 0; i < item.getResources().size(); i++){
			this.resourceIds[i] = item.getResources().get(i).getId();
		}
	}
}