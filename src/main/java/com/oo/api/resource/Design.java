package com.oo.api.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.oo.api.APIClient;
import com.oo.api.OOInstance;
import com.oo.api.ResourceObject;
import com.oo.api.exception.OneOpsClientAPIException;
import com.oo.api.util.JsonUtil;


public class Design extends APIClient {

	String DESIGN_RELEASE_URI;
	String DESIGN_URI;
	OOInstance instance;
	String assemblyName;
	
	public Design(OOInstance instance, String assemblyName) throws OneOpsClientAPIException {
		super(instance);
		if(assemblyName == null || assemblyName.length() == 0) {
			String msg = String.format("Missing assembly name");
			throw new OneOpsClientAPIException(msg);
		}
		this.assemblyName = assemblyName;
		this.instance = instance;
		DESIGN_RELEASE_URI = Assembly.ASSEMBLY_URI + assemblyName + "/design/releases/";
		DESIGN_URI = Assembly.ASSEMBLY_URI + assemblyName + "/design/";
	}
	
	/**
	 * Fetches specific platform details
	 * 
	 * @param platformName
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath getPlatform(String platformName) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to fetch details");
			throw new OneOpsClientAPIException(msg);
		}
		
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_URI + "platforms/" + platformName);
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to get platform with name %s due to %s", platformName, response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to get platform with name %s due to null response", platformName);
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Lists all the platforms
	 * 
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath listPlatforms() throws OneOpsClientAPIException {
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_URI + "platforms");
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to get list of platforms due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to get list of platforms due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	

	/**
	 * Creates platform within the given assembly
	 * 
	 * @param platformName {mandatory}
	 * @param packname {mandatory}
	 * @param packversion {mandatory}
	 * @param packsource {mandatory}
	 * @param comments
	 * @param description
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath createPlatform(String platformName, String packname, String packversion, 
			String packsource, String comments, String description) throws OneOpsClientAPIException {
		
		ResourceObject ro = new ResourceObject();
		Map<String ,String> attributes = new HashMap<String ,String>();
		Map<String ,String> properties= new HashMap<String ,String>();
		
		if(platformName != null && platformName.length() > 0) {
			properties.put("ciName", platformName);
		} else {
			String msg = String.format("Missing platform name to create platform");
			throw new OneOpsClientAPIException(msg);
		}
		
		if(packname != null && packname.length() > 0) {
			attributes.put("pack", packname);
		} else {
			String msg = String.format("Missing pack name to create platform");
			throw new OneOpsClientAPIException(msg);
		}
		
		if(packversion != null && packversion.length() > 0) {
			attributes.put("version", packversion);
		} else {
			String msg = String.format("Missing pack version to create platform");
			throw new OneOpsClientAPIException(msg);
		}
		
		if(packsource != null && packsource.length() > 0) {
			attributes.put("source", packsource);
		} else {
			String msg = String.format("Missing platform name to create platform");
			throw new OneOpsClientAPIException(msg);
		}
		attributes.put("major_version", "1");
		
		properties.put("comments", comments);
		ro.setProperties(properties);
		
		attributes.put("description", description);
		ro.setAttributes(attributes);
		
		Map<String, String> ownerProps = Maps.newHashMap();
		ownerProps.put("description", "");
		ro.setOwnerProps(ownerProps );
		RequestSpecification request = createRequest();
		JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
		Response response = request.body(jsonObject.toString()).post(DESIGN_URI + "platforms/");
		
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to create platform with name %s due to %s", platformName, response.getBody().asString());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to create platform with name %s due to null response", platformName);
		throw new OneOpsClientAPIException(msg);
	}
	
	

	/**
	 * Commits design open releases
	 * 
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath commitDesign() throws OneOpsClientAPIException {
		
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_RELEASE_URI + "latest");
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				
				String releaseState = response.getBody().jsonPath().get("releaseState");
				if("open".equals(releaseState)) {
					int releaseId = response.getBody().jsonPath().get("releaseId");
					response = request.post(DESIGN_RELEASE_URI + releaseId + "/commit");
					if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
						return response.getBody().jsonPath();
					} else {
						String msg = String.format("Failed to commit design due to %s",  response.getStatusLine());
						throw new OneOpsClientAPIException(msg);
					}
				} else {
					String msg = String.format("No open release found to perform design commit");
					System.out.println(msg);
					return response.getBody().jsonPath();
				}
				
				
			} else {
				String msg = String.format("Failed to get latest release details due to %s",  response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to commit design due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Deletes the given platform
	 * 
	 * @param platformName
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath deletePlatform(String platformName) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to delete");
			throw new OneOpsClientAPIException(msg);
		}
		
		RequestSpecification request = createRequest();
		Response response = request.delete(DESIGN_URI + "platforms/" + platformName);
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to delete platform with name %s due to %s", platformName, response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to delete platform with name %s due to null response", platformName);
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * List platform components for a given assembly/design/platform
	 * 
	 * @param environmentName
	 * @param platformName
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath listPlatformComponents(String platformName) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to list enviornment platform components");
			throw new OneOpsClientAPIException(msg);
		}
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_URI + "platforms/" + platformName + "/components");
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to get list of platforms components due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to get list of platforms components due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Get platform component details for a given assembly/design/platform
	 * 
	 * @param platformName
	 * @param componentName
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath getPlatformComponent(String platformName, String componentName) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to get platform component details");
			throw new OneOpsClientAPIException(msg);
		}
		if(componentName == null || componentName.length() == 0) {
			String msg = String.format("Missing component name to get platform component details");
			throw new OneOpsClientAPIException(msg);
		}
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_URI + "platforms/" + platformName + "/components/" + componentName);
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to get platform component details due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to get platform component details due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Add component to a given assembly/design/platform
	 * 
	 * @param platformName
	 * @param componentName
	 * @param attributes
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath addPlatformComponent(String platformName, String componentName, String uniqueName, Map<String, String> attributes) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to add component");
			throw new OneOpsClientAPIException(msg);
		}
		if(componentName == null || componentName.length() == 0) {
			String msg = String.format("Missing component name to add component");
			throw new OneOpsClientAPIException(msg);
		}

		RequestSpecification request = createRequest();
		
		Response newComponentResponse = request.queryParam("template_name", componentName).get(DESIGN_URI + "platforms/" + platformName + "/components/new.json");
		if(newComponentResponse != null) {
			ResourceObject ro = new ResourceObject();
			Map<String, String> properties = Maps.newHashMap();
			properties.put("ciName", uniqueName);
			properties.put("rfcAction", "add");
			
			JsonPath componentDetails = newComponentResponse.getBody().jsonPath();
			Map<String, String> attr = componentDetails.getMap("ciAttributes");
			if(attr == null) {
				attr = Maps.newHashMap();
			}
			if(attributes != null && attributes.size() > 0) {
				attr.putAll(attributes);
				
				Map<String, String> ownerProps =  componentDetails.getMap("ciAttrProps.owner");
				if(ownerProps == null) {
					ownerProps = Maps.newHashMap();
				}
				for(Entry<String, String> entry :  attributes.entrySet()) {
					ownerProps.put(entry.getKey(), "");
				}
				ro.setOwnerProps(ownerProps);
			}
			ro.setAttributes(attr);
			ro.setProperties(properties);
			JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
			jsonObject.put("template_name", componentName);
			Response response = request.body(jsonObject.toString()).post(DESIGN_URI + "platforms/" + platformName + "/components/" );
			if(response != null) {
				if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
					return response.getBody().jsonPath();
				} else {
					String msg = String.format("Failed to get update component %s due to %s", componentName, response.getBody().prettyPrint() + DESIGN_URI + "platforms/" + platformName + "/components/");
					throw new OneOpsClientAPIException(msg);
				}
			} 
		} 
		
		String msg = String.format("Failed to get update component %s due to null response", componentName);
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Update component attributes for a given assembly/design/platform/component
	 * 
	 * @param platformName
	 * @param componentName
	 * @param attributes
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath updatePlatformComponent(String platformName, String componentName, Map<String, String> attributes) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to update component attributes");
			throw new OneOpsClientAPIException(msg);
		}
		if(componentName == null || componentName.length() == 0) {
			String msg = String.format("Missing component name to update component attributes");
			throw new OneOpsClientAPIException(msg);
		}
		if(attributes == null || attributes.size() == 0) {
			String msg = String.format("Missing attributes list to be updated");
			throw new OneOpsClientAPIException(msg);
		}
		
		JsonPath componentDetails = getPlatformComponent(platformName, componentName);
		if(componentDetails != null) {
			ResourceObject ro = new ResourceObject();
			
			String ciId = componentDetails.getString("ciId");
			RequestSpecification request = createRequest();
			Map<String, String> attr = componentDetails.getMap("ciAttributes");
			if(attr == null) {
				attr = Maps.newHashMap();
			}
			attr.putAll(attributes);
			
			Map<String, String> ownerProps =  componentDetails.getMap("ciAttrProps.owner");
			if(ownerProps == null) {
				ownerProps = Maps.newHashMap();
			}
			for(Entry<String, String> entry :  attributes.entrySet()) {
				ownerProps.put(entry.getKey(), "design");
			}
			ro.setOwnerProps(ownerProps);
			ro.setAttributes(attr);
			JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
 			Response response = request.body(jsonObject.toString()).put(DESIGN_URI + "platforms/" + platformName + "/components/" + ciId);
			if(response != null) {
				if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
					return response.getBody().jsonPath();
				} else {
					String msg = String.format("Failed to get update component %s due to %s", componentName, response.getStatusLine());
					throw new OneOpsClientAPIException(msg);
				}
			} 
		}
		String msg = String.format("Failed to get update component %s due to null response", componentName);
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * List local variables for a given assembly/design/platform
	 * 
	 * @param platformName
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath listPlatformVariables(String platformName) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to list platform variables");
			throw new OneOpsClientAPIException(msg);
		}
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_URI + "platforms/" + platformName + "/variables");
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to get list of design platforms variables due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to get list of design platforms variables due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Add platform variables for a given assembly/design
	 * 
	 * @param environmentName
	 * @param variables
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath addPlatformVariable(String platformName, Map<String, String> variables, boolean isSecure) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to add variables");
			throw new OneOpsClientAPIException(msg);
		}
		if(variables == null || variables.size() == 0) {
			String msg = String.format("Missing variables list to be added");
			throw new OneOpsClientAPIException(msg);
		}
		
		RequestSpecification request = createRequest();
		for(Entry<String, String> entry : variables.entrySet()) {
			
			Response variable = request.get(DESIGN_URI + "platforms/" + platformName + "/variables/" + entry.getKey());
			if(variable != null && variable.getBody() != null && variable.getBody().jsonPath().getString("ciId") != null) {
				String msg = String.format("Global variables %s already exists", entry.getKey());
				throw new OneOpsClientAPIException(msg);
			}
			ResourceObject ro = new ResourceObject();
			Response newVarResponse = request.get(DESIGN_URI + "platforms/" + platformName + "/variables/new.json");
			if(newVarResponse != null) {
				JsonPath newVarJsonPath = newVarResponse.getBody().jsonPath();
				if(newVarJsonPath != null) {
					Map<String, String> attr = newVarJsonPath.getMap("ciAttributes");
					Map<String, String> properties = Maps.newHashMap();
					if(attr == null) {
						attr = Maps.newHashMap();
					}
					if(isSecure) {
						attr.put("secure", "true");
						attr.put("encrypted_value", entry.getValue());
					} else {
						attr.put("secure", "false");
						attr.put("value", entry.getValue());
					}
					
					properties.put("ciName", entry.getKey());
					ro.setProperties(properties);
					ro.setAttributes(attr);
				}
			}
			
			JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
			
			Response response = request.body(jsonObject.toString()).post(DESIGN_URI + "platforms/" + platformName + "/variables" );
			if(response != null) {
				if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
					return response.getBody().jsonPath();
				} else {
					String msg = String.format("Failed to add platform variable %s due to %s", entry.getKey(), response.getStatusLine());
					throw new OneOpsClientAPIException(msg);
				}
			} 
		}
		
		String msg = String.format("Failed to add new variables %s due to null response", variables);
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Update platform local variables for a given assembly/design/platform
	 * 
	 * @param platformName
	 * @param variables
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public Boolean updatePlatformVariable(String platformName, Map<String, String> variables, boolean isSecure) throws OneOpsClientAPIException {
		if(platformName == null || platformName.length() == 0) {
			String msg = String.format("Missing platform name to update variables");
			throw new OneOpsClientAPIException(msg);
		}
		if(variables == null || variables.size() == 0) {
			String msg = String.format("Missing variables list to be updated");
			throw new OneOpsClientAPIException(msg);
		}
		
		Boolean success = false;
		RequestSpecification request = createRequest();
		for(Entry<String, String> entry : variables.entrySet()) {
			
			Response variable = request.get(DESIGN_URI + "platforms/" + platformName + "/variables/" + entry.getKey());
			if(variable == null || variable.getBody() == null) {
				String msg = String.format("Failed to find local variables %s for platform %s", entry.getKey(), platformName);
				throw new OneOpsClientAPIException(msg);
			}
			
			JsonPath variableDetails = variable.getBody().jsonPath();
			String ciId = variableDetails.getString("ciId");
			Map<String, String> attr = variableDetails.getMap("ciAttributes");
			if(attr == null) {
				attr = new HashMap<String, String> ();
			}
			
			if(isSecure) {
				attr.put("secure", "true");
				attr.put("encrypted_value", entry.getValue());
			} else {
				attr.put("secure", "false");
				attr.put("value", entry.getValue());
			}
			
			ResourceObject ro = new ResourceObject();
			ro.setAttributes(attr);
			
			JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
			
			Response response = request.body(jsonObject.toString()).put(DESIGN_URI + "platforms/" + platformName + "/variables/" + ciId);
			if(response != null) {
				if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
					//return response.getBody().jsonPath();
					success = true;
				} else {
					String msg = String.format("Failed to get update variables %s due to %s", entry.getKey(), response.getStatusLine());
					throw new OneOpsClientAPIException(msg);
				}
			} 
		}
		
		return success;
	}
	
	

	/**
	 * List global variables for a given assembly/design
	 * 
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath listGlobalVariables() throws OneOpsClientAPIException {
		RequestSpecification request = createRequest();
		Response response = request.get(DESIGN_URI + "variables");
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to get list of design variables due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to get list of design variables due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Add global variables for a given assembly/design
	 * 
	 * @param environmentName
	 * @param variables
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath addGlobalVariable(Map<String, String> variables, boolean isSecure) throws OneOpsClientAPIException {
		if(variables == null || variables.size() == 0) {
			String msg = String.format("Missing variables list to be added");
			throw new OneOpsClientAPIException(msg);
		}
		
		RequestSpecification request = createRequest();
		for(Entry<String, String> entry : variables.entrySet()) {
			
			Response variable = request.get(DESIGN_URI + "variables/" + entry.getKey());
			if(variable != null && variable.getBody() != null && variable.getBody().jsonPath().getString("ciId") != null) {
				String msg = String.format("Global variables %s already exists", entry.getKey());
				throw new OneOpsClientAPIException(msg);
			}
			ResourceObject ro = new ResourceObject();
			Response newVarResponse = request.get(DESIGN_URI + "variables/new.json");
			if(newVarResponse != null) {
				JsonPath newVarJsonPath = newVarResponse.getBody().jsonPath();
				if(newVarJsonPath != null) {
					Map<String, String> attr = newVarJsonPath.getMap("ciAttributes");
					Map<String, String> properties = Maps.newHashMap();
					if(attr == null) {
						attr = Maps.newHashMap();
					}
					if(isSecure) {
						attr.put("secure", "true");
						attr.put("encrypted_value", entry.getValue());
					} else {
						attr.put("secure", "false");
						attr.put("value", entry.getValue());
					}

					properties.put("ciName", entry.getKey());
					ro.setProperties(properties);
					ro.setAttributes(attr);
				}
			}
			
			JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
			
			Response response = request.body(jsonObject.toString()).post(DESIGN_URI + "variables" );
			if(response != null) {
				if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
					return response.getBody().jsonPath();
				} else {
					String msg = String.format("Failed to get new global variable %s due to %s", entry.getKey(), response.getStatusLine());
					throw new OneOpsClientAPIException(msg);
				}
			} 
		}
		
		String msg = String.format("Failed to add new variables %s due to null response", variables);
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Update global variables for a given assembly/design
	 * 
	 * @param environmentName
	 * @param variables
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public Boolean updateGlobalVariable(Map<String, String> variables, boolean isSecure) throws OneOpsClientAPIException {
		if(variables == null || variables.size() == 0) {
			String msg = String.format("Missing variables list to be updated");
			throw new OneOpsClientAPIException(msg);
		}
		Boolean success = false;
		RequestSpecification request = createRequest();
		for(Entry<String, String> entry : variables.entrySet()) {
			
			Response variable = request.get(DESIGN_URI + "variables/" + entry.getKey());
			if(variable == null || variable.getBody() == null) {
				String msg = String.format("Failed to find global variables %s", entry.getKey());
				throw new OneOpsClientAPIException(msg);
			}
			JsonPath variableDetails = variable.getBody().jsonPath();
			String ciId = variableDetails.getString("ciId");
			Map<String, String> attr = variableDetails.getMap("ciAttributes");
			if(attr == null) {
				attr = new HashMap<String, String> ();
			}
			if(isSecure) {
				attr.put("secure", "true");
				attr.put("encrypted_value", entry.getValue());
			} else {
				attr.put("secure", "false");
				attr.put("value", entry.getValue());
			}
			
			ResourceObject ro = new ResourceObject();
			ro.setAttributes(attr);
			
			JSONObject jsonObject = JsonUtil.createJsonObject(ro , "cms_dj_ci");
			
			Response response = request.body(jsonObject.toString()).put(DESIGN_URI + "variables/" + ciId);
			if(response != null) {
				if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
					success = true;
				} else {
					String msg = String.format("Failed to get update global variable %s due to %s", entry.getKey(), response.getStatusLine());
					throw new OneOpsClientAPIException(msg);
				}
			} 
		}
		
		return success;
	}
	
	/**
	 * Fetches specific platform details in Yaml format
	 * 
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath extractYaml() throws OneOpsClientAPIException {
		RequestSpecification request = createRequest();
		
		Response response = request.get(DESIGN_URI + "extract.yaml");
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to extract yaml content due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to extract yaml content due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
	/**
	 * Adds specific platform from Yaml/Json file input
	 * 
	 * @param platformName
	 * @return
	 * @throws OneOpsClientAPIException
	 */
	public JsonPath loadFile(String filecontent) throws OneOpsClientAPIException {
		
		if(filecontent == null || filecontent.length() == 0) {
			String msg = String.format("Missing input file content");
			throw new OneOpsClientAPIException(msg);
		}
		
		RequestSpecification request = createRequest();
		request.header("Content-Type", "multipart/text");
		MultipartEntityBuilder meb = MultipartEntityBuilder.create();
		meb.addTextBody("data", filecontent);
		JSONObject jo = new JSONObject();
		jo.put("data", filecontent);
		
		Response response = request.parameter("data", filecontent).put(DESIGN_URI + "load" );
		if(response != null) {
			if(response.getStatusCode() == 200 || response.getStatusCode() == 302) {
				return response.getBody().jsonPath();
			} else {
				String msg = String.format("Failed to load yaml content due to %s", response.getStatusLine());
				throw new OneOpsClientAPIException(msg);
			}
		} 
		String msg = String.format("Failed to load yaml content due to null response");
		throw new OneOpsClientAPIException(msg);
	}
	
}