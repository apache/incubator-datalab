package com.epam.dlab.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateNotebookDto {
    
	private String image;
    private String name;
    @JsonProperty("template_name")
    private String templateName;
    private String shape;
    private String version;
    

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getShape() {
        return shape;
    }
    
    public void setShape(String shape) {
        this.shape = shape;
    }
    
	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public CreateNotebookDto(){
        
    }

}
       