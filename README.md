# O11n-deploy-maven-plugin

A Maven plug-in that helps you develop Java plug-ins for VMware vRealize Orchestrator by automatically installing the compiled *.vmoapp or *.dar files on the configured vRealize Orchestrator server.

**Note: due to the lack of a API endpoint to restart the orchestration service you will still have to restart it manually e.g. using *service vco-server restart*.** You may further automate the deployment by executing a *SSH* command to do the job once the build was successfull.


## Install

1.Add Repository
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

2.Add dependency
```xml
	<dependency>
	    <groupId>com.github.gvart</groupId>
	    <artifactId>o11n-deploy-maven-plugin</artifactId>
	    <version>0.1.3</version>
	</dependency>
```

## Usage
This Mojo should be configured within your *o11nplugin-**pluginname**/pom.xml* Maven module. It has a single goal **deployplugin** and usually you should run it in the **install** phase. The **deployplugin** goal has the following parameters:

- **o11nServer**: VMware Orchestrator server hostname or IP-address. **Required**.
- **o11nPort**: VMware Orchestrator server REST API port. Defaults to *8281*.
- **o11nUser**: User with sufficient permissions to import plugins. **Required**.
- **o11nPassword**: Password for the provided user. **Required**.
- **o11nOverwrite**: Set 'true' if you want to overwrite the plugin in case it already exists. Defaults to *true*.
- **o11nPluginType**: Either 'vmoapp' or 'dar' depending on the plug-in format. Defaults to *vmoapp*.
- **o11nPluginFilePath**: Path to the plugin *.vmoapp or *.dar that should be installed. Defaults to *${project.build.directory}*.
- **o11nPluginFileName**: Filename without file extension of the plugin that should be installed. Defaults to *${project.build.finalName}*.
- **o11nDeletePackage**: Default value is 'false' in case that is set to 'true' package and all related elements( workflows, actions ) will be deleted before installing

All parameters are provided as Strings and converted into the required format internally. A simple `mvn install` will then trigger the upload of the compiled plugin.


### Example configuration
A example that uses all currently available parameters.

```xml
<plugin>
	<groupId>com.github.gvart</groupId>
	<artifactId>o11n-deploy-maven-plugin</artifactId>
	<version>0.1.2</version>
	<executions>
		<execution>
			<phase>install</phase>
			<goals>
				<goal>deployplugin</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<o11nServer>localhost</o11nServer>
		<o11nPort>8281</o11nPort>
		<o11nUser>vcoadmin</o11nUser>
		<o11nPassword>vcoadmin</o11nPassword>
		<o11nOverwrite>true</o11nOverwrite>
		<o11nDeletePackage>true</o11nDeletePackage>
		<o11nPluginType>vmoapp</o11nPluginType>
		<o11nPluginFilePath>${project.build.directory}</o11nPluginFilePath>
		<o11nPluginFileName>${project.build.finalName}</o11nPluginFileName>
	</configuration>
</plugin>
```


### Example execution
 
 An example output of a successfull run may look like this:
```bash
[INFO] --- o11n-deploy-maven-plugin:0.1.1:deployplugin (default) @ o11nplugin-coopto ---
[INFO] Configured plugin: 'D:\workspace\coopto\o11nplugin-coopto\target\o11nplugin-coopto-0.0.3-dev.vmoapp'.
[INFO] Configured server: 'https://localhost:8281/vco/api/plugins'.
[INFO] HTTP 204. Successfully updated plug-in in VMware Orchestrator!
```


## Licensing & Legal
O11n-deploy-maven-plugin – from now on “this project”, “this program” or “this software” – is an open source project.

*This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.*

*This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.*

*You should have received a copy of the GNU Lesser General Public License along with this program. If not, see http://www.gnu.org/licenses/.*


This software may include *“Open Source Software”*, which means various open source software components licensed under the terms of applicable open source license agreements included in the materials relating to such software. Open Source Software is composed of individual software components, each of which has its own copyright and its own applicable license conditions. Information about the used Open Source Software and their licenses can be found in the *pom.xml* file. The Product may also include other components, which may contain additional open source software packages. One or more such *license* files may therefore accompany this Product.

It is your responsibility to ensure that your use and/or transfer does not violate applicable laws. 

All product and company names are trademarks ™ or registered ® trademarks of their respective holders. Use of them does not imply any affiliation with or endorsement by them.