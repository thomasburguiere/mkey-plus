package fr.lis.mkeyplusWSREST.model;

import fr.lis.xper3API.model.Resource;

/**
 * 
 * @author antoine
 *
 */
public class JsonResource{

	/**
	 * 
	 */
	private String name;
	private String author;
	private String type;
	private String url;
	private long id;
	private String legend;
	private String keywords;
	
	public JsonResource(Resource resource){
		this.name = resource.getName();
		this.url = resource.getUrl();
		this.author = resource.getAuthor();
		this.type = resource.getType();
		this.id = resource.getId();
		this.legend = resource.getLegend();
		this.keywords = resource.getKeywords();
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getLegend() {
		return legend;
	}

	public void setLegend(String legend) {
		this.legend = legend;
	}


	public String getKeywords() {
		return keywords;
	}


	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
}