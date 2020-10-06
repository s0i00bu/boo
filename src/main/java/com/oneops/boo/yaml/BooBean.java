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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BooBean {
  @JsonProperty("oneops_host")
  private String host;
  @JsonProperty("organization")
  private String org;
  @JsonProperty("api_key")
  private String apikey;
  private String email;
  @JsonProperty("cloud")
  private Map<String, Object> cloud;

  @JsonProperty("ip_output")
  private String ipOutput;

  @JsonProperty("environment_name")
  private String envName;

  @JsonProperty("description")
  private String description;

  @JsonProperty(value = "gzip_enabled", defaultValue = "true")
  private boolean gzipEnabled = true;

  @JsonProperty("comments")
  private String comments;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  @JsonProperty("enable_delivery")
  private boolean enable;

  @JsonProperty("custom")
  private Map<String, String> custom;

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public String getIpOutput() {
    return ipOutput;
  }

  public void setIpOutput(String ipOutput) {
    this.ipOutput = ipOutput;
  }

  /**
   * Get the host string and if it does not end with a slash concatenate it with one.
   * 
   */
  public String getHost() {
    if (host != null && !host.endsWith("/")) {
      return host.concat("/");
    }
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getOrg() {
    return org;
  }

  public void setOrg(String org) {
    this.org = org;
  }

  public String getApikey() {
    return apikey;
  }

  public void setApikey(String apikey) {
    this.apikey = apikey;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Map<String, Object> getCloud() {
    return cloud;
  }

  public void setCloud(Map<String, Object> cloud) {
    this.cloud = cloud;
  }

  public Map<String, String> getCustom() {
    return custom;
  }

  public void setCustom(Map<String, String> custom) {
    this.custom = custom;
  }

  public boolean isGzipEnabled() {
    return gzipEnabled;
  }

  public void setGzipEnabled(boolean enableGzip) {
    this.gzipEnabled = enableGzip;
  }
}
