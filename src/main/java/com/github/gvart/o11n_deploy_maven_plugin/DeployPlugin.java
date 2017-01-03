/*	This file is part of project "o11n-deploy-maven-plugin", a computer software	* 
 *  plugin for deploying Java plugins to VMware vRealize Orchestrator using			*
 *  Maven build management.															*
 *																					*
 *																					*
 *	Copyright (C) 2016 Robert Szymczak	(m451@outlook.com)				        	*
 *																					*
 *																					*
 *	This program is free software: you can redistribute it and/or modify			*
 *	it under the terms of the GNU Lesser General Public License as published 		*
 *	by the Free Software Foundation, either version 3 of the License, or			*
 *	(at your option) any later version.												*
 *																					*
 *	This program is distributed in the hope that it will be useful,					*
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of					*
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  							*
 *	See the GNU Lesser General Public License for more details.						*
 *																					*
 *	You should have received a copy of the GNU Lesser General Public License		*
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.			*/

package com.github.gvart.o11n_deploy_maven_plugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

/**
 * Mojo which deploys a created VMware Orchestrator plug-in to the configured VMware Orchestrator Server.
 * This Mojo should be configured within your o11nplugin-PLUGINNAME/pom.xml Maven module.
 */
@Mojo(name = "deployplugin", defaultPhase = LifecyclePhase.INSTALL)
public class DeployPlugin extends AbstractMojo {

	// Taken from Maven API through PluginParameterExpressionEvaluator
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;
	
	//Server Configuration
	@Parameter(defaultValue = "localhost", property = "deployplugin.server", required = true)		// VMware Orchestrator Server Hostname or IP
	private String o11nServer;
	@Parameter(defaultValue = "8281", property = "deployplugin.port")	// VMware Orchestrator Server REST API Port
	private Integer o11nPort;
	@Parameter(defaultValue = "vcoadmin", property = "deployplugin.user", required = true)			// User with sufficient permissions to import plug-ins
	private String o11nUser;
	@Parameter(defaultValue = "vcoadmin", property = "deployplugin.password", required = true)		// Password for the provided user
	private String o11nPassword;

	//Plug-in Configuration
	@Parameter(defaultValue = "${project.build.directory}", property = "deployplugin.pluginpath")	// Path to the plug-in *.vmoapp or *.dar that should be installed
	private String o11nPluginFilePath;
	@Parameter(defaultValue = "${project.build.finalName}", property = "deployplugin.pluginfile")	// Filename without file extension of the plug-in that should be installed
	private String o11nPluginFileName;
	@Parameter(defaultValue = "vmoapp", property = "deployplugin.plugintype")	// Either 'vmoapp' or 'dar' depending on the plug-in format
	private String o11nPluginType;
	@Parameter(defaultValue = "true", property = "deployplugin.overwrite")	// Set 'true' if you want to overwrite the plug-in in case it already exists
	private boolean o11nOverwrite;
	@Parameter(defaultValue = "false", property = "deployplugin.deletepackage") //Set 'true' to delete package and all elements stored in it.
	private boolean o11nDeletePackage;

	public void execute() throws MojoExecutionException, MojoFailureException
    {
    	//Force set all non-required parameters in case user accidently set them null
		Build build = project.getBuild();
		if(o11nPluginFilePath == null || o11nPluginFilePath.isEmpty())
		{
			o11nPluginFilePath = build.getDirectory();
		}

		if(o11nPluginFileName == null || o11nPluginFilePath.isEmpty())
		{
			o11nPluginFileName = build.getFinalName() ;
		}
		if(o11nPluginType == null || o11nPluginType.isEmpty())
		{
			//may be dar or vmoapp
			o11nPluginType = "vmoapp";
		}
		if(o11nPort == null || o11nPort < 1 || o11nPort >65535)
		{
			o11nPort = 8281;
		}
		
		//Example: D:\Workspace\coopto\o11nplugin-coopto\target\o11nplugin-PLUGINNAME-0.1.vmoapp
		//also works for UNIX based OS
		String plugin = o11nPluginFilePath + File.separator + o11nPluginFileName + "." + o11nPluginType;
		getLog().info("Configured plugin: '" + plugin + "'.");
		File file = new File(plugin);
		
		
    	//Example: https://localhost:8281/vco/api/plugins
		//Other method in question: /api/plugins/installPluginDynamically - not used due to missing docs and source. Not sure what's the difference. Seems not to support "overwrite".
    	String url = "https://" + o11nServer + ":" + o11nPort.toString() + "/vco/api/plugins";
    	getLog().info("Configured server: '" + url + "'.");


		// BEGIN -- Allow Self-Signed vRO-Certificates
    	// TODO build in option to provide the trusted certificate
		SSLContext sslContext;
		try
		{
			sslContext = new SSLContextBuilder()
			        .loadTrustMaterial(null, (x509CertChain, authType) -> true)
			        .build();
		} catch (Exception e)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			throw new MojoFailureException("ERROR CREATING SSL-CONTEXT FOR ORCHESTRATOR-PLUGIN-UPLOAD:\n" + sw.getBuffer().toString());
		}
		
		CloseableHttpClient httpclient = HttpClients
			.custom()
		    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
		    .setSSLContext(sslContext)
			.build();
		
		Unirest.setHttpClient(httpclient);
		// END -- Allow Self-Signed vRO-Certificates

		try {
			if(o11nDeletePackage) {
				getLog().info("START DELETING!");
				String delUrl = "https://" + o11nServer + ":" + o11nPort.toString() + "/vco/api/packages/";
				delUrl += project.getArtifact().getGroupId() + "." + project.getArtifactId().split("o11nplugin-")[1]
						+ "/?option=deletePackageKeepingShared";

				HttpResponse<String> response =
						Unirest.delete(delUrl)
								.header("accept", "application/json")
								.basicAuth(o11nUser, o11nPassword)
								.asString();

				logInfo(response.getStatus());
			}

			HttpResponse<String> response2 = Unirest.post(url)
					.header("accept", "application/json")
					.basicAuth(o11nUser, o11nPassword)
					.field("format", o11nPluginType)
					.field("overwrite", String.valueOf(o11nOverwrite))
					.field("file", file)
					.asString();

			logInfo(response2.getStatus());

		} catch (Exception e)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			throw new MojoFailureException("ERROR TRYING TO EXECUTE UPLOAD-ORCHESTRATOR-PLUGIN REQUEST:\n" + sw.getBuffer().toString());
		}
	}

	private void logInfo(int statusCode){
		switch (statusCode)
		{
			case 200:
				getLog().info("HTTP 200. Successfully deleted package in VMware Orchestrator.");
				break;
			case 201:
				getLog().info("HTTP 201. Successfully updated plug-in in VMware Orchestrator.");
				break;
			case 204:
				getLog().info("HTTP 204. Successfully updated plug-in in VMware Orchestrator.");
				break;
			case 401:
				getLog().info("HTTP 401. Authentication is required to upload a plug-in.");
				break;
			case 403:
				getLog().info("HTTP 403. The provided user is not authorized to upload a plug-in.");
				break;
			case 404:
				getLog().info("HTTP 404. The requested resource was not found. Make sure you entered the correct VMware Orchestrator URL and that VMware Orchestrator is reachable under that URL from the machine running this Maven Mojo.");
				break;
			case 409:
				getLog().info("HTTP 409. The provided plug-in already exists and the overwrite flag was not set. The plug-in will not be changed in VMware Orchestrator.");
				break;
			default:
				getLog().warn("Unknown status code HTTP " + statusCode + " returned from VMware Orchestrator. Please verify if the plug-in has been updated successfully. I really got no clue.");
				break;
		}
	}
}
