/**
 * (C) Copyright IBM Corporation 2020, 2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.openliberty.tools.common.plugins.util.LibertyPropFilesUtility;
import io.openliberty.tools.maven.utils.CommonLogger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.openliberty.tools.ant.FeatureManagerTask.Feature;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil;
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties;
import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms;
import io.openliberty.tools.common.plugins.util.PluginExecutionException;
import io.openliberty.tools.common.plugins.util.PluginScenarioException;
import io.openliberty.tools.maven.server.types.Features;
import io.openliberty.tools.maven.server.types.Key;


public abstract class InstallFeatureSupport extends ServerFeatureSupport {

    /**
     * Define a set of features to install in the server and the configuration
     * to be applied for all instances.
     */
    @Parameter
    protected Features features;
    
    /**
     * key ID and key URL pair
     */
    @Parameter(property = "keys")
    private Map<String, Key> keys;
    
    

    public boolean noFeaturesSection = false;

    public boolean installFromAnt;
    
    public boolean installManually = false;

    protected InstallFeatureUtil util;
    
    public static final String FEATURES_JSON_ARTIFACT_ID = "features";

    protected class InstallFeatureMojoUtil extends InstallFeatureUtil {
        public InstallFeatureMojoUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons, Collection<Map<String,String>> keyMap)
                throws PluginScenarioException, PluginExecutionException {
            super(installDirectory, new File(project.getBuild().getDirectory()), features.getFrom(), features.getTo(), pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons, features.getVerify(), keyMap);
            setContainerEngine(this);
        }

        @Override
        public void debug(String msg) {
            getLog().debug(msg);
        }
        
        @Override
        public void debug(String msg, Throwable e) {
            getLog().debug(msg, e);
        }
        
        @Override
        public void debug(Throwable e) {
            getLog().debug(e);
        }
        
        @Override
        public void warn(String msg) {
            getLog().warn(msg);
        }

        @Override
        public void info(String msg) {
            getLog().info(msg);
        }
        
        @Override
        public boolean isDebugEnabled() {
            return getLog().isDebugEnabled();
        }

        @Override
        public void error(String msg) {
            getLog().error(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            getLog().error(msg, e);
        }
        
        @Override
        public File downloadArtifact(String groupId, String artifactId, String type, String version) throws PluginExecutionException {
            try {
                return getArtifact(groupId, artifactId, type, version).getFile();
            } catch (MojoExecutionException e) {
                throw new PluginExecutionException(e);
            }
        }
        
        @Override
        public File downloadSignature(File esa, String groupId, String artifactId, String type, String version) throws PluginExecutionException {
        	return downloadArtifact(groupId, artifactId, type, version);
        }
    }

    protected Set<String> getPluginListedFeatures(boolean findEsaFiles) {
        Set<String> result = new HashSet<String>();
        for (Feature feature : features.getFeatures()) {
            if ((findEsaFiles && feature.getFeature().endsWith(".esa"))
                    || (!findEsaFiles && !feature.getFeature().endsWith(".esa"))) {
                result.add(feature.getFeature());
                getLog().debug("Plugin listed " + (findEsaFiles ? "ESA" : "feature") + ": " + feature.getFeature());
            }
        }
        return result;
    }
    
    protected Set<String> getDependencyFeatures() {
        Set<String> result = new HashSet<String>();
        List<org.apache.maven.model.Dependency> dependencyArtifacts = project.getDependencies();
        for (org.apache.maven.model.Dependency dependencyArtifact: dependencyArtifacts){
            if (("esa").equals(dependencyArtifact.getType())) {
                result.add(dependencyArtifact.getArtifactId());
                getLog().debug("Dependency feature: " + dependencyArtifact.getArtifactId());
            }
        }
        return result;
    }
    
    protected List<String> getAdditionalJsonList() {
        //note in this method we use the BOM coordinate but replace the BOM artifactId
        //with the features JSON artifactId which by prepare-feature convention is "features"
        List<String> result = new ArrayList<String>();
        org.apache.maven.model.DependencyManagement dependencyManagement = project.getDependencyManagement();
        if(dependencyManagement == null) {
        	getLog().debug("Features-bom is not provided by the user");
        	return null;
        }
        List<org.apache.maven.model.Dependency> dependencyManagementArtifacts = dependencyManagement.getDependencies();
        for (org.apache.maven.model.Dependency dependencyArtifact: dependencyManagementArtifacts){
            if (("pom").equals(dependencyArtifact.getType()) && ("features-bom").equals(dependencyArtifact.getArtifactId())) {
                String coordinate = String.format("%s:%s:%s",
                        dependencyArtifact.getGroupId(), FEATURES_JSON_ARTIFACT_ID, dependencyArtifact.getVersion());
                result.add(coordinate);
                getLog().debug("Features-bom is provided by the user");
                getLog().info("Additional user feature json coordinate: " + coordinate);
            }
        }
        return result;
    }
    
    protected Collection<Map<String, String>> getKeyMap(){ 	
    	Collection<Map<String,String>> keyMapList = new ArrayList<>(); 
    	if(keys != null) {
    		for(Key k: keys.values()) {
    			Map<String, String> keyMap = new HashMap<>();
        		getLog().debug("Key Id: " + k.getKeyid() +" Key URL: " + k.getKeyurl());
        		keyMap.put("keyid", k.getKeyid());
        		keyMap.put("keyurl", k.getKeyurl());
        		keyMapList.add(keyMap);
        	}
    	}
    	return keyMapList;
    }

    protected boolean initialize() throws MojoExecutionException {
        if (skip) {
            getLog().info("\nSkipping install-feature goal.\n");
            return false;
        }

        if (features == null) {
            // For liberty-assembly integration:
            // When using installUtility, if no features section was specified, 
            // then don't install features because it requires license acceptance
            noFeaturesSection = true;
            
            // initialize features section for all scenarios except for the above
            features = new Features();
        }

        checkServerHomeExists();

        return true;
    }

    /**
     * Get the current specified Liberty features.
     *
     * @param containerName The container name if the features should be installed in a container. Otherwise null.
     * @return Set of Strings containing the specified Liberty features
     */
    protected FeaturesPlatforms getSpecifiedFeatures(String containerName) throws PluginExecutionException {
        Set<String> pluginListedFeatures = getPluginListedFeatures(false);

        if (util == null) {
            Set<String> pluginListedEsas = getPluginListedFeatures(true);
            util = getInstallFeatureUtil(pluginListedEsas, containerName);
        }

        if (util == null && noFeaturesSection) {
            //No features were installed because acceptLicense parameter was not configured
            return new FeaturesPlatforms();
        }
        else if (util == null && !noFeaturesSection) {
            Set<String> featuresToInstall = new HashSet<String>();
            for (Feature feature : features.getFeatures()) {
                featuresToInstall.add(feature.toString());
            }
            return new FeaturesPlatforms(featuresToInstall, new HashSet<String>());
        }
        else {
            Set<String> dependencyFeatures = getDependencyFeatures();
            Set<String> serverFeatures = new HashSet<String>();
            Set<String> serverPlatforms = new HashSet<String>();
            FeaturesPlatforms getServerResult = serverDirectory.exists() ? util.getServerFeatures(serverDirectory, LibertyPropFilesUtility.getLibertyDirectoryPropertyFiles(new CommonLogger(getLog()), installDirectory, userDirectory, serverDirectory, new File(outputDirectory, serverName))) : null;
            if (getServerResult != null) {
            	serverFeatures = getServerResult.getFeatures();
            	serverPlatforms = getServerResult.getPlatforms();
            }
            
            return new FeaturesPlatforms(util.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures),serverPlatforms);
            
        }
    }

    private void createNewInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons, Collection<Map<String,String>> keyMap) 
            throws PluginExecutionException {
        try {
            util = new InstallFeatureMojoUtil(pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons, keyMap);
        } catch (PluginScenarioException e) {
            getLog().debug(e.getMessage());
            if (noFeaturesSection) {
                getLog().debug("Skipping feature installation with installUtility because the "
                        + "features configuration element with an acceptLicense parameter "
                        + "was not specified for the install-feature goal.");
            } else if(additionalJsons != null && !additionalJsons.isEmpty()) {
            	getLog().debug("Skipping feature installation with installUtility because it is not supported for user feature");
        	}else {
                installFromAnt = true;
                getLog().debug("Installing features from installUtility.");
            }
        }
    }

    /**
     * Get a new instance of InstallFeatureUtil
     * 
     * @param pluginListedEsas The list of ESAs specified in the plugin configuration, or null if not specified
     * @param containerName The container name if the features should be installed in a container. Otherwise null.
     * @return instance of InstallFeatureUtil
     */
    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas, String containerName)
            throws PluginExecutionException {
        List<ProductProperties> propertiesList = null;
        String openLibertyVersion = null;
        if (containerName == null) {
            propertiesList = InstallFeatureUtil.loadProperties(installDirectory);
            openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList);
        }
        List<String> additionalJsons = getAdditionalJsonList();
        Collection<Map<String,String>> keyMap = getKeyMap();
        return getInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons, keyMap);
    }

    /**
     * Get a new instance of InstallFeatureUtil
     * 
     * @param pluginListedEsas The list of ESAs specified in the plugin configuration, or null if not specified
     * @param propertiesList The list of product properties installed with the Open Liberty runtime
     * @param openLibertyVersion The version of the Open Liberty runtime
     * @param containerName The container name if the features should be installed in a container. Otherwise null.
     * @param additionalJsons Collection of Strings for additional jsons for feature install
     * @return instance of InstallFeatureUtil
     */
    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVersion, String containerName, List<String> additionalJsons, Collection<Map<String,String>> keyMap)
            throws PluginExecutionException {
    	getLog().info("Feature signature verify option: " + features.getVerify());

        createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons, keyMap);
        return util;
    }

}
