package com.wm.bfd.oo.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Uninterruptibles;
import com.jayway.restassured.path.json.JsonPath;
import com.oo.api.OOInstance;
import com.oo.api.exception.OneOpsClientAPIException;
import com.oo.api.resource.Assembly;
import com.oo.api.resource.Design;
import com.oo.api.resource.Operation;
import com.oo.api.resource.Transition;
import com.wm.bfd.oo.ClientConfig;
import com.wm.bfd.oo.utils.ProgressBar;

public abstract class AbstractWorkflow {
    private static Logger LOG = LoggerFactory.getLogger(AbstractWorkflow.class);
    String assemblyName;
    String platformName;
    String envName;

    Assembly assembly;
    Design design;
    Transition transition;
    Operation op;

    ClientConfig config;

    OOInstance instance;

    ProgressBar bar;

    public AbstractWorkflow(OOInstance instance, String assemblyName,
	    String platformName, String envName, ClientConfig config)
	    throws OneOpsClientAPIException {
	this.assemblyName = assemblyName;
	this.platformName = platformName;
	this.envName = envName;
	this.instance = instance;
	this.config = config;

	assembly = new Assembly(instance);
	design = new Design(instance, assemblyName);
	transition = new Transition(instance, assemblyName);
	op = new Operation(instance, assemblyName, envName);
	this.config = config;

	this.bar = new ProgressBar();
    }

    public boolean process() throws OneOpsClientAPIException {
	return false;
    }

    public boolean cleanup() {
	if (design == null)
	    return true;
	this.bar.update(5, 100);
	this.cancelDeployment();
	this.bar.update(20, 100);
	this.disableAllPlatforms();
	this.bar.update(40, 100);
	try {
	    transition.deleteEnvironment(envName);
	} catch (Exception e) {
	    // Ignore
	    e.printStackTrace();
	}
	this.bar.update(60, 100);
	this.deleteDesign();
	// Don't add the following part to one try block as transition.
	try {
	    assembly.deleteAssembly(assemblyName);
	} catch (Exception e) {
	    // Ignore
	}
	this.bar.update(90, 100);
	op = null;
	design = null;
	assembly = null;
	this.bar.update(100, 100);
	return true;
    }

    void cancelDeployment() {
	try {
	    JsonPath response = transition.getLatestDeployment(envName);
	    String deploymentId = response.getString("deploymentId");
	    response = transition.getLatestRelease(envName);
	    String releaseId = response.getString("releaseId");
	    LOG.debug("deploymentId:" + deploymentId + "; releaseId: "
		    + releaseId);
	    response = transition.getDeploymentStatus(envName, deploymentId);
	    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
	    response = transition.cancelDeployment(envName, deploymentId,
		    releaseId);

	    LOG.debug("cancelDeployment: "
		    + (response == null ? "" : response.prettyPrint()));
	} catch (Exception e) {
	    // Ignore
	}

    }

    void disableAllPlatforms() {
	try {
	    transition.disableAllPlatforms(envName);
	    transition.commitEnvironment(envName, null, "Clean up " + envName);
	    transition.deploy(envName, "test deploy!");
	} catch (Exception e) {
	    // Ignore
	}
    }

    void deleteDesign() {
	JsonPath response = null;
	try {
	    LOG.debug("deleteEnvironment log:"
		    + (response == null ? "" : response.prettyPrint()));
	    response = design.commitDesign();
	    LOG.debug("commitDesign log:"
		    + (response == null ? "" : response.prettyPrint()));
	    design.deletePlatform(platformName);
	} catch (Exception e) {
	    // Ignore
	    e.printStackTrace();
	}
    }

    public boolean isAssemblyExist() {
	JsonPath response = null;
	try {
	    response = assembly.getAssembly(assemblyName);
	} catch (OneOpsClientAPIException e) {
	    String msg = String.format("The assembly %s is not exist!",
		    assemblyName);
	    System.err.println(msg);
	}
	return response == null ? false : true;
    }

    public boolean createAssemblyIfNotExist() throws OneOpsClientAPIException {
	boolean isExist = this.isAssemblyExist();
	if (!isExist) {
	    assembly.createAssembly(assemblyName, config.getConfig().getBoo()
		    .getEmail(), "", "");
	}
	return true;
    }

    public boolean isEnvExist() {
	JsonPath response = null;
	try {
	    response = transition.getEnvironment(envName);
	} catch (OneOpsClientAPIException e) {
	    String msg = String.format("The environment %s is not exist! %s",
		    platformName, e.getMessage());
	    System.err.println(msg);
	}
	return (response == null ? false : true);
    }

    public boolean createEnv() throws OneOpsClientAPIException {
	boolean isExist = this.isEnvExist();
	JsonPath response = null;
	if (!isExist) {
	    Map<String, String> cloudMap = new HashMap<String, String>();
	    cloudMap.put(config.getConfig().getBoo().getCloudId(), "1");
	    response = transition.createEnvironment(envName, "DEV",
		    "redundant", null, cloudMap, false, true, "");
	    response = transition.getEnvironment(envName);

	    transition.commitEnvironment(envName, null,
		    "Committed by bfd oneops automation!");
	}
	return response == null ? false : true;
    }

    public boolean deploy() throws OneOpsClientAPIException {

	JsonPath response = transition.deploy(envName,
		"Created by bfd oneops automation!");
	return response == null ? false : true;
    }
}