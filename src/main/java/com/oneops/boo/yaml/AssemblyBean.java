/*
 * Copyright 2017 Walmart, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.oneops.boo.yaml;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssemblyBean {
  
  @JsonProperty("name")
	private String name;

	@JsonProperty("auto_gen")
	private Boolean autoGen;
	
	@JsonProperty("description")
	private String description;

	@JsonProperty("tags")
	private Map<String, String> tags;

	@JsonProperty("teams")
	private List<String> teams;

	@JsonProperty("site")
	private List<String> site;

	public List<String> getSite() {
		return site;
	}

	public void setSite(List<String> site) {
		this.site = site;
	}

	public List<String> getTeams() {
		return teams;
	}

	public void setTeams(List<String> teams) {
		this.teams = teams;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getAutoGen() {
		return autoGen == null ? Boolean.FALSE : autoGen;
	}

	public void setAutoGen(Boolean autoGen) {
		this.autoGen = autoGen;
	}

	public final Map<String, String> getTags() {
		return tags;
	}

	public final void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String description) {
		this.description = description;
	}
}
