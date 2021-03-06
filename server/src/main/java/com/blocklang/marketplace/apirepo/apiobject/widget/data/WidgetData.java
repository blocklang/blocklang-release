package com.blocklang.marketplace.apirepo.apiobject.widget.data;

import java.util.ArrayList;
import java.util.List;

import com.blocklang.marketplace.apirepo.apiobject.ApiObject;
import com.fasterxml.jackson.annotation.JsonIgnore;

//Widget changelog 生成的结果结构
// 通常 CreateXXX 的属性与 XXX 的属性完全一致
//ChangeData, Codeable
public class WidgetData implements ApiObject {
	private String id; // widget 的唯一标识，对应目录名中的时间戳
	private String code;
	private String name;
	private String label;
	private String description;
	private List<WidgetProperty> properties = new ArrayList<WidgetProperty>();
	private List<WidgetEvent> events = new ArrayList<WidgetEvent>();
	// TODO: 支持 slots
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<WidgetProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<WidgetProperty> properties) {
		this.properties = properties;
	}

	public List<WidgetEvent> getEvents() {
		return events;
	}

	public void setEvents(List<WidgetEvent> events) {
		this.events = events;
	}

	/**
	 * 获取属性编码
	 * 
	 * <p> widget 中的属性和事件使用相同的编码序列，将事件看作特殊的属性。</p>
	 * 
	 * @return 返回最大属性编码，如果还未包含属性或事件则返回 "0"
	 */
	@JsonIgnore
	public String getMaxPropertyCode() {
		String propertyMaxSeed = "0";
		if(!properties.isEmpty()) {
			propertyMaxSeed = properties.get(properties.size() - 1).getCode();
		}
		String eventMaxSeed = "0";
		if(!events.isEmpty()) {
			eventMaxSeed = events.get(events.size() - 1).getCode();
		}
		
		return propertyMaxSeed.compareTo(eventMaxSeed) >= 0 ? propertyMaxSeed : eventMaxSeed;
	}
}
