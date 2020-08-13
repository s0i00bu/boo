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
package com.oneops.boo.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.jayway.restassured.path.json.JsonPath;
import com.oneops.api.OOInstance;
import com.oneops.api.exception.OneOpsClientAPIException;
import com.oneops.api.resource.Assembly;
import com.oneops.api.resource.Cloud;
import com.oneops.api.resource.Design;
import com.oneops.api.resource.Operation;
import com.oneops.api.resource.Transition;
import com.oneops.api.resource.model.CiResource;
import com.oneops.api.resource.model.Deployment;
import com.oneops.api.resource.model.Procedure;
import com.oneops.api.resource.model.Release;
import com.oneops.boo.BooConfig;
import com.oneops.boo.LogUtils;
import com.oneops.boo.utils.ProgressBar;
import com.oneops.boo.yaml.AssemblyBean;
import com.oneops.boo.yaml.CloudBean;
import com.oneops.boo.yaml.Constants;
import com.oneops.boo.yaml.EnvironmentBean;
import com.oneops.boo.yaml.PlatformBean;
import com.oneops.boo.yaml.helper.EnvironmentBeanHelper;

/**
 * Common methods in work flow.
 */
public abstract class AbstractWorkflow {

  /** The log. */
  private static Logger LOG = LoggerFactory.getLogger(AbstractWorkflow.class);

  /** The pattern. */
  private static Pattern pattern = Pattern.compile("^-[0-9a-zA-Z]{2,9}$");

  /** The Assembly Bean */
  AssemblyBean assemblyBean;
  
  /** The assembly. */
  Assembly assembly;

  /** The design. */
  Design design;

  /** The transition. */
  Transition transition;

  /** The config. */
  BooConfig config;

  /** The instance. */
  OOInstance instance;

  /** The cloud. */
  Cloud cloud;

  /** The bar. */
  ProgressBar bar;

  /** The comments. */
  String comments = null;

  /**
   * Instantiates a new abstract workflow.
   *
   * @param instance the instance
   * @param config the config
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public AbstractWorkflow(OOInstance instance, BooConfig config, String comment)
      throws OneOpsClientAPIException {

    this.instance = instance;
    this.config = config;
    this.assemblyBean = config.getYaml().getAssembly();
    this.cloud = new Cloud(instance);
    
    String assemblyName = assemblyBean.getName();

    assembly = new Assembly(instance);
    design = new Design(instance, assemblyName);
    transition = new Transition(instance, assemblyName);
    this.config = config;

    this.bar = new ProgressBar();
    if (!StringUtils.isBlank(comment)) {
      this.comments = comment;
    }
  }

  /**
   * Process.
   *
   * @param isUpdate the is update
   * @param isAssemblyOnly the is assembly only
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public abstract List<Deployment> process(boolean isUpdate, boolean isAssemblyOnly)
      throws OneOpsClientAPIException;

  
  /**
   * Cleanup.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean cleanup() throws OneOpsClientAPIException, InterruptedException {
	  if(this.isAssemblyExist()) {
		  removeAllEnvs();
		  while (this.getStatus() != null && this.getStatus().contains("active")) {
			LogUtils.info("Env deployment still in progress");
	        TimeUnit.SECONDS.sleep(10);
	      }
		  return deleteAssembly();
	  }
	  return true;
  }
  /**
   * Removes the all envs.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<Deployment> removeAllEnvs() throws OneOpsClientAPIException {
    if (design == null) {
      return null;
    }
    
    List<Deployment> deployments = new ArrayList<>();
    List<String> envList = listEnvs();
    envList.forEach(env -> {
    	this.cancelDeployment(env);
        Deployment deployment = this.disableAllPlatforms(env);
        if (deployment != null) {
          deployments.add(deployment);
        }
        try {
          transition.deleteEnvironment(env);
        } catch (Exception e) {
          // Do nothing
        }
    });
   	
    return deployments;
  }

  /**
   * Removes the all platforms.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean removeAllPlatforms() throws OneOpsClientAPIException {
    if (design == null) {
      return true;
    }
    boolean isSuc = true;
    for (String platformName : this.listPlatforms()) {
      try {
        design.deletePlatform(platformName);
      } catch (Exception e) {
        // Do nothing
        isSuc = false;
      }
    }
    if (isSuc) {
      isSuc = this.deleteAssembly();
    }
    return isSuc;
  }

  /**
   * Gets the config.
   *
   * @return the config
   */
  public BooConfig getConfig() {
    return config;
  }

  /**
   * List envs.
   *
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listEnvs() throws OneOpsClientAPIException {
    List<CiResource> response = transition.listEnvironments();
    List<String> envs = Lists.newArrayList();
    for(CiResource resource : response) {
    	envs.add(resource.getCiName());
    }
    return envs;
  }

  /**
   * List platforms.
   *
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listPlatforms() throws OneOpsClientAPIException {
    List<CiResource> response = design.listPlatforms();
    List<String> platforms = Lists.newArrayList();
    for(CiResource resource : response) {
    	platforms.add(resource.getCiName());
    }
    return platforms;
  }

  /**
   * Delete assembly.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  private boolean deleteAssembly() throws OneOpsClientAPIException {
    return this.deleteAssembly(this.assemblyBean.getName());
  }

  /**
   * Delete assembly.
   *
   * @param assemblyName the assembly name
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  private boolean deleteAssembly(String assemblyName) throws OneOpsClientAPIException {
    // Don't add the following part to one try block as transition.
    assembly.deleteAssembly(assemblyName);
    LogUtils.info(Constants.DESTROY_ASSEMBLY, assemblyName);
    assembly = null;
    return true;
  }

  /**
   * Gets the assemblies.
   *
   * @return the assemblies
   * @throws OneOpsClientAPIException the one ops client API exception
   */
	public List<String> getAssemblies() throws OneOpsClientAPIException {
		List<CiResource> response = assembly.listAssemblies();
		List<String> assemblies = Lists.newArrayList();
		for (CiResource resource : response) {
			assemblies.add(resource.getCiName());
		}
		return assemblies;
	}

  /**
   * List attachments.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listAttachments(String platformName, String componentName)
      throws OneOpsClientAPIException {
    List<CiResource> response = design.listPlatformComponentAttachments(platformName, componentName);
    List<String> names = new ArrayList<String>();
    for (CiResource ciResource : response) {
    	names.add(ciResource.getCiName());
	}
	  return names;
  }

  /**
   * Adds the attachment.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @param uniqueName the unique name
   * @param attributes the attributes
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean addAttachment(String platformName, String componentName, String uniqueName,
      Map<String, String> attributes) throws OneOpsClientAPIException {
	  
    design.addNewAttachment(platformName, componentName, uniqueName, attributes);
    return true;
  }

  /**
   * Update attachment.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @param uniqueName the unique name
   * @param attributes the attributes
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean updateAttachment(String platformName, String componentName, String uniqueName,
      Map<String, String> attributes) throws OneOpsClientAPIException {
    design.updatePlatformComponentAttachment(platformName, componentName, uniqueName, attributes);
    return true;
  }

  /**
   * Checks if is attachment exists.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @param attachmentName the attachment name
   * @return true, if is attachment exists
   */
  public boolean isAttachmentExists(String platformName, String componentName,
      String attachmentName) {
    boolean isExist = true;
    CiResource response = null;
    try {
      response = design.getPlatformComponentAttachment(platformName, componentName, attachmentName);
    } catch (Exception e) {
      isExist = false;
    }
    return (response == null || !isExist) ? false : true;
  }

  /**
   * Checks whether the component is user related.
   *
   */
  public boolean isUserCustomizedComponent(String platformName, String componentName)
      throws OneOpsClientAPIException {
    CiResource componentDetails = design.getPlatformComponent(platformName, componentName);
    if(componentDetails != null && componentDetails.getAttrProps() != null 
    		&& componentDetails.getAttrProps().getAdditionalProperties() != null 
    		&& componentDetails.getAttrProps().getAdditionalProperties().get(Constants.OWNER) != null) {
    	@SuppressWarnings("unchecked")
		Map<String, String> ownersMap = (Map<String, String>) componentDetails.getAttrProps().getAdditionalProperties().get(Constants.OWNER);
    	if(ownersMap != null && ownersMap.size() > 0)
    		return true;
    }
      return false;
  }
  
  /**
   * Checks whether the component is user related.
   *
   */
  public boolean isUserCustomizedVariable(String platformName, String variableName)
      throws OneOpsClientAPIException {
    CiResource variableDetails = design.getPlatformVariable(platformName, variableName);
    if(variableDetails != null && variableDetails.getAttrProps() != null 
    		&& variableDetails.getAttrProps().getAdditionalProperties() != null 
    		&& variableDetails.getAttrProps().getAdditionalProperties().get(Constants.OWNER) != null) {
    	@SuppressWarnings("unchecked")
		Map<String, String> ownersMap = (Map<String, String>) variableDetails.getAttrProps().getAdditionalProperties().get(Constants.OWNER);
    	if(ownersMap != null && ownersMap.size() > 0)
    		return true;
    }
      return false;
  }



  /**
   * Cancel deployment.
   *
   * @param envName the env name
   * @return true, if successful
   */
  boolean cancelDeployment(String envName) {
    boolean isSuc = false;
    try {
      Deployment response = transition.getLatestDeployment(envName);
      Long deploymentId = response.getDeploymentId();
      Release release = transition.getLatestRelease(envName);
      Long releaseId = release.getReleaseId();
      if (LOG.isDebugEnabled()) {
        LOG.debug("deploymentId:" + deploymentId + "; releaseId: " + releaseId);
      }
      response = transition.getDeploymentStatus(envName, deploymentId);
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
      response = transition.cancelDeployment(envName, deploymentId, releaseId);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Cancel deployment: " + (response == null ? "" : response.getComments()));
      }
      isSuc = true;
    } catch (Exception e) {
      // Ignore
    }

    return isSuc;

  }

  /**
   * Sometimes we have to retry a few times to make the deployment done.
 * @param envName 
   *
   * @return true, if successful
   */
	public Deployment retryDeployment(String envName) {
        long deploymentId = 0;
		try {
			Deployment response = transition.getLatestDeployment(envName);
			deploymentId = response.getDeploymentId();
			Release release = transition.getLatestRelease(envName);
			Long releaseId = release.getReleaseId();
			if (LOG.isDebugEnabled()) {
				LOG.debug("deploymentId:" + deploymentId + "; releaseId: " + releaseId);
			}
			response = transition.getDeploymentStatus(envName, deploymentId);
			if(response != null && "complete".equalsIgnoreCase(response.getDeploymentState())) {
				LOG.info("Deployment already completed");
			} else {
				response = transition.retryDeployment(envName, deploymentId, releaseId);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Retry deployment: " + (response == null ? "" : response.getComments()));
				}
			}
			return response;
		} catch (Exception e) {
          LOG.warn("Error while submitting a retry deployment with id: " + deploymentId);
		}

		return null;
	}

  /**
   * Disable all platforms.
   *
   * @param envName the env name
   */
  Deployment disableAllPlatforms(String envName) {
    try {
      transition.disableAllPlatforms(envName);
      transition.commitEnvironment(envName, null, "Clean up " + envName);
      if (StringUtils.isBlank(this.comments)) {
        return transition.deploy(envName, Constants.CLEANUP_DESCRIPTION);
      } else {
        return transition.deploy(envName, comments);
      }
    } catch (Exception e) {
      // Ignore
    }
    return null;
  }

  /**
   * Delete design.
   *
   * @param platformName the platform name
   */
  void deleteDesign(String platformName) {
    Release response = null;
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("deleteEnvironment log:" + (response == null ? "" : response.getReleaseState()));
      }
      response = design.commitDesign();
      if (LOG.isDebugEnabled()) {
        LOG.debug("commitDesign log:" + (response == null ? "" : response.getReleaseState()));
      }
      design.deletePlatform(platformName);
    } catch (Exception e) {
      // Ignore
      e.printStackTrace();
    }
  }

  /**
   * Checks if is assembly exist.
   *
   * @return true, if is assembly exist
   */
  public boolean isAssemblyExist() {
    return this.isAssemblyExist(this.assemblyBean.getName());
  }

  /**
   * Checks if is assembly exist.
   *
   * @param assemblyName the assembly name
   * @return true, if is assembly exist
   */
  public boolean isAssemblyExist(String assemblyName) {
    CiResource response = null;
    try {
      response = assembly.getAssembly(assemblyName);
    } catch (OneOpsClientAPIException e) {
      // String msg = String.format("The assembly %s is not exist!", assemblyName);
      // System.err.println(msg);
      // Ignore
    }
    return response == null ? false : true;
  }

  /**
   * List actions.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listActions(String envName, String platformName, String componentName)
      throws OneOpsClientAPIException {
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    JsonPath response = op.listActions(platformName, componentName);
    return response.getList("actionName");
  }
  
  /**
   * List actions.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listActions(String platformName, String componentName)
      throws OneOpsClientAPIException {
	
	String envName = null;
	List<EnvironmentBean> environmentList = this.config.getYaml().getEnvironmentList();
	if(environmentList != null && environmentList.size() > 0) {
		envName = environmentList.get(0).getEnvName();
	}
	if(envName == null) {
		throw new OneOpsClientAPIException("No valid environment found!!!");
	}
	  
    Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    JsonPath response = op.listActions(platformName, componentName);
    return response.getList("actionName");
  }

  /**
   * List instances map.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the map
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public Map<String, Long> listInstancesMap(String envName, String platformName, String componentName)
      throws OneOpsClientAPIException {
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    List<CiResource> response = op.listInstances(platformName, componentName);
    Map<String, Long> map = new HashMap<String, Long>();
    for (CiResource ciResource : response) {
		map.put(ciResource.getCiName(), ciResource.getCiId());
	}
    
    return map;
  }

  /**
   * List instances.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listInstances(String envName, String platformName, String componentName)
      throws OneOpsClientAPIException {
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    List<CiResource> response = op.listInstances(platformName, componentName);
    List<String> names = new ArrayList<String>();
    for (CiResource ciResource : response) {
		names.add(ciResource.getCiName());
	}
   
    return names;
  }
  
  /**
   * List instances.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<String> listInstances(String platformName, String componentName)
      throws OneOpsClientAPIException {
	  
	String envName = null;
	List<EnvironmentBean> environmentList = this.config.getYaml().getEnvironmentList();
	if(environmentList != null && environmentList.size() > 0) {
		envName = environmentList.get(0).getEnvName();
	}
	if(envName == null) {
		throw new OneOpsClientAPIException("No valid environment found!!!");
	}
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    List<CiResource> response = op.listInstances(platformName, componentName);
    List<String> names = new ArrayList<String>();
    for (CiResource ciResource : response) {
		names.add(ciResource.getCiName());
	}
   
    return names;
  }

  /**
   * Gets the procedure status.
   *
   * @param procedureId the procedure id
   * @return the procedure status
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public String getProcedureStatus(String envName, Long procedureId) throws OneOpsClientAPIException {
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    Procedure response = op.getProcedureStatus(procedureId);
    return response.getProcedureState();
  }

  /**
   * List instance ids.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the list
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  private List<Long> listInstanceIds(String envName, String platformName, String componentName)
      throws OneOpsClientAPIException {
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    List<CiResource> response = op.listInstances(platformName, componentName);
    List<Long> Ids = new ArrayList<Long>();
    for (CiResource ciResource : response) {
    	Ids.add(ciResource.getCiId());
	}
    return Ids;
  }

  /**
   * Execute action.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @param actionName the action name
   * @param arglist the arglist
   * @param instanceList the instance list
   * @param rollAt the roll at
   * @return the string
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public Long executeAction(String envName, String platformName, String componentName, String actionName,
      String arglist, List<String> instanceList, int rollAt) throws OneOpsClientAPIException {
    List<Long> list = new ArrayList<Long>();
    if (instanceList == null || instanceList.size() == 0) {

      list = this.listInstanceIds(envName, platformName, componentName);
    } else {
      Map<String, Long> map = this.listInstancesMap(envName, platformName, componentName);
      for (String name : instanceList) {
        list.add(map.get(name));
      }
    }
    Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    Procedure response =
        op.executeAction(platformName, componentName, actionName, list, arglist, rollAt);
    return response == null ? null : response.getProcedureId();
  }


  /**
   * Checks if is platforms exist.
   *
   * @return true, if is platforms exist
   */
  public boolean isPlatformsExist() {
    List<CiResource> response = null;
    try {
      response = design.listPlatforms();
    } catch (OneOpsClientAPIException e) {
      // String msg = String.format("The assembly %s is not exist!", assemblyName);
      // System.err.println(msg);
      // Ignore
    }
    return response == null ? false : true;
  }

  /**
   * Platform exist.
   *
   * @param platformName the platform name
   * @return true, if successful
   */
  public boolean platformExist(String platformName) {
    CiResource response = null;
    try {
      response = design.getPlatform(platformName);
    } catch (OneOpsClientAPIException e) {
      // Ignore
    }
    return response == null ? false : true;
  }

  /**
   * Creates the assembly if not exist.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean createAssemblyIfNotExist() throws OneOpsClientAPIException {
    boolean isExist = this.isAssemblyExist();
    if (!isExist) {
      this.checkAssemblyName();
      LOG.debug("creating assembly {} with tags: {}", assemblyBean.getName(), assemblyBean.getTags());
      assembly.createAssembly(assemblyBean.getName(), config.getYaml().getBoo().getEmail(), "", assemblyBean.getDescription(), assemblyBean.getTags());
        if (assemblyBean.getTeams()!= null && !assemblyBean.getTeams().isEmpty()){
            LOG.debug("creating assembly {} with teams: {}", assemblyBean.getName(), assemblyBean.getTeams());
            assembly.addTeamsByAssembly(config.getYaml().getBoo().getOrg(), assemblyBean.getName(), assemblyBean.getTeams());
        }
    }
    return true;
  }

  /**
   * Check assembly name.
   */
  void checkAssemblyName() {
    if (this.assemblyBean.getName().length() > 32) {
      System.err.println();
      System.err.println(Constants.ASSEMBLY_NAME_TOO_LONG);
      System.exit(3);
    }
  }

  /**
   * Checks if is match.
   *
   * @param str the str
   * @param prefix the prefix
   * @return true, if is match
   */
  private boolean isMatch(String str, String prefix) {
    if (str.startsWith(prefix)) {
      Matcher matcher = pattern.matcher(str.substring(prefix.length()));
      if (matcher.matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the all auto gen assemblies.
   *
   * @param prefix the prefix
   * @return the all auto gen assemblies
   */
  public List<String> getAllAutoGenAssemblies(String prefix) {

    if (config.getYaml().getAssembly().getAutoGen()) {
      try {
        List<String> assemblies = this.getAssemblies();
        if (assemblies != null && assemblies.size() > 0) {
          List<String> matches = new ArrayList<String>();
          for (String assembly : assemblies) {
            // Only match auto generated string.
            if (this.isMatch(assembly, prefix)) {
              matches.add(assembly);
            }
          }
          return matches;
        }
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    } else {
      List<String> matches = new ArrayList<String>();
      if (this.isAssemblyExist(prefix)) {
        matches.add(prefix);
      }
      return matches;
    }
    return null;
  }

  /**
   * Checks if is env exist.
   *
   * @param platformName the platform name
   * @return true, if is env exist
   */
  public boolean isEnvExist(String envName) {
    CiResource response = null;
    try {
      response = transition.getEnvironment(envName);
    } catch (OneOpsClientAPIException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(Constants.ENV_NOT_EXISTING, envName, e.getMessage());
      }
    }
    return (response == null ? false : true);
  }

  /**
   * Creates the env.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean createEnv(EnvironmentBean environmentBean) throws OneOpsClientAPIException {
	String envName = environmentBean.getEnvName();
    boolean isExist = this.isEnvExist(envName);
    CiResource response = null;
    if (!isExist) {

      LogUtils.info(Constants.CREATE_ENV, envName);

      Map<String, Map<String, String>> cloudMaps = new HashMap<String, Map<String, String>>();

//      EnvironmentBean environmentBean = config.getYaml().getEnvironmentBean();
	List<CloudBean> clouds = environmentBean.getClouds();
      for (CloudBean cloud : clouds) {
        Map<String, String> cloudMap = new HashMap<String, String>();
        cloudMap.put(EnvironmentBeanHelper.PRIORITY, cloud.getPriority());
        cloudMap.put(EnvironmentBeanHelper.DPMT_ORDER, cloud.getDpmtOrder());
        cloudMap.put(EnvironmentBeanHelper.PCT_SCALE, cloud.getPctScale());
        cloudMaps.put(String.valueOf(getCloudId(cloud.getCloudName())), cloudMap);
      }

      response = transition.createEnvironment(envName,
    		  environmentBean.getOthers().get(Constants.ENV_PROFILE),
          environmentBean.getOthers(), null, cloudMaps,
          Constants.DESCRIPTION);
      
      response = transition.getEnvironment(envName);
      if (StringUtils.isBlank(this.comments)) {
        transition.commitEnvironment(envName, null, Constants.DESCRIPTION);
      } else {
        transition.commitEnvironment(envName, null, comments);
      }

    } else {
      LogUtils.info(Constants.ENV_EXISTING, envName);
    }
    return response == null ? false : true;
  }


  /**
   * Update env.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  @SuppressWarnings("unchecked")
  public boolean updateEnv(EnvironmentBean environmentBean) throws OneOpsClientAPIException {
    List<PlatformBean> platforms = environmentBean.getPlatformsList();
    if (platforms == null) {
      return false;
    }
    for (PlatformBean platform : platforms) {
      Map<String, Object> map = platform.getComponents();
      if (map != null) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          transition.updatePlatformComponent(environmentBean.getEnvName(), platform.getName(), entry.getKey(),
              (Map<String, String>) entry.getValue());
        }
      }
    }
    return true;
  }

  /**
   * Update platform cloud scale.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean updatePlatformCloudScale(EnvironmentBean environmentBean) throws OneOpsClientAPIException {
    for (PlatformBean platform : this.config.getYaml().getPlatformsList()) {
      if (this.platformExist(platform.getName())) {
        CiResource environment = transition.getEnvironment(environmentBean.getEnvName());
        @SuppressWarnings("unchecked")
		Map<String, Object> sysClouds = (Map<String, Object>) environment.getAdditionalProperties().get(Constants.CLOUDS);
        List<CloudBean> clouds = environmentBean.getClouds();
        for (CloudBean cloud : clouds) {
        	String cloudId = String.valueOf(this.getCloudId(cloud.getCloudName()));
          if (sysClouds.containsKey(cloudId)) {
            Map<String, String> cloudMap = new HashMap<String, String>();
            cloudMap.put(EnvironmentBeanHelper.ADMINSTATUS, Constants.ACTIVE);
            cloudMap.put(EnvironmentBeanHelper.PRIORITY, cloud.getPriority());
            cloudMap.put(EnvironmentBeanHelper.DPMT_ORDER, cloud.getDpmtOrder());
            cloudMap.put(EnvironmentBeanHelper.PCT_SCALE, cloud.getPctScale());
            transition.updatePlatformCloudScale(environmentBean.getEnvName(), platform.getName(),
                String.valueOf(getCloudId(cloud.getCloudName())), cloudMap);
            // If cloud exists in yaml, remove the cloud name from the system clouds map after
            // updating cloud.
            sysClouds.remove(cloudId);
          }
        }
        // For rest clouds not in yaml, set them as shutdown.
        for (String cloud : sysClouds.keySet()) {
          Map<String, String> cloudMap = new HashMap<String, String>();
          cloudMap.put(EnvironmentBeanHelper.ADMINSTATUS, Constants.OFFLINE);
          transition.updatePlatformCloudScale(environmentBean.getEnvName(), platform.getName(), cloud, cloudMap);
        }
      }
    }
    return true;
  }

  /**
   * Update platform auto healing options.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean updatePlatformHealingOptions(EnvironmentBean environmentBean) throws OneOpsClientAPIException {
	  List<PlatformBean> platforms = environmentBean.getPlatformsList();
	  if (platforms == null) {
	      return false;
	  }
	  Operation op = new Operation(instance, this.assemblyBean.getName(), environmentBean.getEnvName());
     for (PlatformBean platform : platforms) {
      if(platform.getAutoHealing() == null || platform.getAutoHealing().isEmpty()) {
    	  return false;
      }
      if (this.platformExist(platform.getName())) {
        int repairCount = -1, repairTime = -1;
        for(Entry<String, Object> entry : platform.getAutoHealing().entrySet()) {
        	if(entry.getKey().startsWith("auto")) {
        		op.updatePlatformAutoHealingStatus(environmentBean.getEnvName(), platform.getName(), entry.getKey(), (Boolean)entry.getValue());
        	}
        	if(PlatformBean.REPLACE_AFTER_MINUTES.equals(entry.getKey())) {
        		repairTime = (Integer)entry.getValue();
        	}
        	if(PlatformBean.REPLACE_AFTER_REPAIRS.equals(entry.getKey())) {
        		repairCount = (Integer)entry.getValue();
        	}
        }
        if(repairCount != -1 && repairTime != -1) {
        	op.updatePlatformAutoReplaceConfig(environmentBean.getEnvName(), platform.getName(), repairCount, repairTime);
        }
      }
    }
    return true;
  }
  
  /**
   * Pull design.
   *
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public void pullDesign(String envName) throws OneOpsClientAPIException {
    transition.pullDesign(envName);
  }

  /**
   * Commit env.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public boolean commitEnv(String envName) throws OneOpsClientAPIException {
    Release response;
    if (StringUtils.isBlank(this.comments)) {
      response = transition.commitEnvironment(envName, null, Constants.DESCRIPTION);
    } else {
      response = transition.commitEnvironment(envName, null, comments);
    }
    return response == null ? false : true;
  }

  /**
   * Deploy.
   *
   * @return true, if successful
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public Deployment deploy(String envName, boolean isUpdate) throws OneOpsClientAPIException {
    Deployment response;
    if (StringUtils.isBlank(this.comments)) {
      if (isUpdate) {
        response = transition.deploy(envName, Constants.UPDATE_DESCRIPTION);
      } else {
        response = transition.deploy(envName, Constants.CREATE_DESCRIPTION);
      }
    } else {
      response = transition.deploy(envName, comments);
    }
    return response;
  }


  /**
   * Get ip address from oneops operate.
   *
   * @param platformName the platform name
   * @param componentName the component name
   * @return the ips internal
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public List<Map<String, Object>> getIpsInternal(String envName, String platformName, String componentName)
      throws OneOpsClientAPIException {
	Operation op = new Operation(instance, this.assemblyBean.getName(), envName);
    List<CiResource> response = op.listInstances(platformName, componentName);
    List<Map<String, Object>> attrList = new ArrayList<Map<String, Object>>();
    for (CiResource ciResource : response) {
    	attrList.add(ciResource.getCiAttributes().getAdditionalProperties());
	}
    return attrList;
  }

  /**
   * Gets the cloud id.
   *
   * @param cloudName the cloud name
   * @return the cloud id
   * @throws OneOpsClientAPIException the one ops client API exception
   */
  public Long getCloudId(String cloudName) throws OneOpsClientAPIException {
    CiResource response = cloud.getCloud(cloudName);
    return response.getCiId();
  }

  /**
   * Gets the deployment id.
   *
   * @return the deployment id
   */
  Long getDeploymentId(String envName) {
	  Long id = null;
    try {
      Deployment latestDeployment = transition.getLatestDeployment(envName);
      id = latestDeployment.getDeploymentId();
    } catch (OneOpsClientAPIException e) {
      // Ignore
    }
    return id;
  }

  /**
   * Gets the status.
   *
   * @return the status
   */
  public String getStatus(String envName) {
    String status = null;
    try {
      Deployment response = transition.getDeploymentStatus(envName, this.getDeploymentId(envName));
      status = response.getDeploymentState();
    } catch (OneOpsClientAPIException e) {
      // Ignore
    }
    return status;
  }
  
  /**
   * Gets the status.
   *
   * @return the status
   */
  public String getStatus() {
    StringBuilder sb = new StringBuilder();
    for(EnvironmentBean eb : this.config.getYaml().getEnvironmentList()) {
    	try {
    	      Deployment response = transition.getDeploymentStatus(eb.getEnvName(), this.getDeploymentId(eb.getEnvName()));
    	      String status = response.getDeploymentState();
    	      sb.append("\n").append(eb.getEnvName()).append(" deployment status:").append(status);
    	    } catch (OneOpsClientAPIException e) {
    	      // Ignore
    	    }
    }
    
    return sb.toString();
  }
}
