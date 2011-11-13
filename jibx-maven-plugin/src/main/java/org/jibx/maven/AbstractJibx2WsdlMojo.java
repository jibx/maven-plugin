/*
 * Copyright (c) 2004-2010, Dennis M. Sosnoski All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of
 * JiBX nor the names of its contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jibx.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jibx.runtime.JiBXException;
import org.jibx.ws.wsdl.tools.Jibx2Wsdl;


/**
 * This is the code that is shared between the BindingMojos.
 *
 * @author                        <a href="mailto:don@tourgeek.com">Don Corley</a>
 */
public abstract class AbstractJibx2WsdlMojo extends AbstractBaseBindingMojo {

    /**
     * The full class names of the service interface.
     * <b>Note:</b> Classes should be in target/classes (which is where they should be after compiling).
     *
     * @parameter  expression="${interfaceClassNames}"
     * @required
     */
    private ArrayList<String> interfaceClassNames;
    
    /**
     * Target directory path for generated output (default is current directory).
     * <b>Note:</b> If you want the wsdl and schema included in your distribution,
     * remember to include it in the &lt;resources&gt; section of your pom file.
     *
     * @parameter  expression="${outputDirectory}" default-value="${project.build.directory}/schema"
     * @required
     */
    private String outputDirectory;
    
    /**
     * Include pattern for customization files.
     *
     * @parameter  expression="${customizations}"
     */
    private ArrayList<String> customizations;
    
    /**
     * The source directories.
     * <b>Note:</b> The source directory defaults to:
     * <code>
     * &lt;sourceDirectories&gt;<br/>
     * &nbsp;&nbsp;&lt;sourceDirectory&gt;src/main/java&lt;/sourceDirectory&gt;<br/>
     * &lt;/sourceDirectories&gt;
     * </code>
     * If you are using the code-gen plugin, you may want to specify the generated sources directory:
     * <code>
     * &lt;sourceDirectories&gt;<br/>
     * &nbsp;&nbsp;&lt;sourceDirectory&gt;src/main/java&lt;/sourceDirectory&gt;<br/>
     * &nbsp;&nbsp;&lt;sourceDirectory&gt;${project.build.directory}/generated-source&lt;/sourceDirectory&gt;<br/>
     * &lt;/sourceDirectories&gt;
     * </code>
     * If you don't want sources include, you will have to explicitly delare an empty list:
     * <code>
     * &lt;sourceDirectories&gt;<br/>
     * &nbsp;&nbsp;&lt;sourceDirectory&gt;&lt;/sourceDirectory&gt;<br/>
     * &lt;/sourceDirectories&gt;
     * </code>
     *
     * @parameter  expression="${sourceDirectories}"
     */
    private ArrayList<String> sourceDirectories;
    
    private static final String DEFAULT_SOURCE_DIRECTORY = "src/main/java";
    
    /**
     * Extra options to be given for customization via CLI.<p/>
     * Enter extra customizations or other command-line options.<br/>
     * The extra customizations are described on the 
     * <a href="/fromcode/jibx2wsdl-customs.html">JiBX2WSDL customizations page</a><br/>
     * The single character JiBX2WSDL commands may also be supplied here.<br/>
     * For example, to include Names of extra classes (-x) and Sets the base address used for the service endpoint
     * address specified in the WSDL, supply the following options:<br/>
     * <code>
     * &lt;options&gt;<br/>
     * &nbsp;&nbsp;&lt;x&gt;com.company.pacakge.ClassName&lt;/x&gt;<br/>
     * &nbsp;&nbsp;&lt;service-base&gt;http://localhost:8080/axis2/services&lt;/service-base&gt;<br/>
     * &lt;/options&gt;
     * </code>
     *
     * @parameter
     */
    private Map<String,String> options;

    /**
     * Determines if running in single- or multi-module mode, collects all bindings and finally runs the binding
     * compiler.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkConfiguration();

        String mode;
        String[] bindings;
        String[] classpaths;

        if ("pom".equalsIgnoreCase(project.getPackaging()))
        {
            getLog().info("Not running JiBX binding compiler for pom packaging");
        	return;		// Don't bind jibx if pom packaging
        }
        
        if (isMultiModuleMode()) {
            if (isRestrictedMultiModuleMode()) {
                mode = "restricted multi-module";
            } else {
                mode = "multi-module";
            }
            bindings = getMultiModuleBindings();
            classpaths = getMultiModuleClasspaths();
        } else {
            mode = "single-module";
            bindings = getSingleModuleBindings();
            classpaths = getSingleModuleClasspaths();
        }

        if (interfaceClassNames.size() == 0) {
            getLog().info("Not running JiBX2WSDL (" + mode + " mode) - no class interface files");
        } else {
            getLog().info("Running JiBX binding compiler (" + mode + " mode) on " + interfaceClassNames.size()
                          + " interface file(s)");
       
            try {
                java.util.List<String> args = new Vector<String>();

                for (int i = 0; i< classpaths.length; i++)
                {
                    args.add("-p");
                	args.add(classpaths[i]);
                }
                
                args.add("-t");
                args.add(outputDirectory);
                
                if (customizations.size() > 0) {
                    args.add("-c");
                    for (String customization : customizations) {
                        args.add(customization);
                    }
                }

                for (Map.Entry<String,String> entry : options.entrySet()) {
                    String option = "--" + entry.getKey() + "=" + entry.getValue();
                    if ((entry.getKey().toString().length() == 1) && (Character.isLowerCase(entry.getKey().toString().charAt(0))))
                    {
                    	getLog().debug("Adding option : -" + entry.getKey() + " " + entry.getValue());
                    	args.add("-" + entry.getKey());
                    	args.add(entry.getValue());
                    }
                    else
                    {
        	            getLog().debug("Adding option: " + option);
        	            args.add(option);
                    }
                }
                if (bindings.length > 0)
                {
                    args.add("-u");
                    StringBuilder arg = new StringBuilder();
                	for (int i = 0 ; i < bindings.length; i++)
                	{
                		if (arg.length() > 0)
                			arg.append(';');
                		arg.append(bindings[i]);
                	}
                	args.add(arg.toString());
                }
                
                if (this.sourceDirectories.size() > 0)
                    if (this.sourceDirectories.get(0) != null)
                        if (this.sourceDirectories.get(0).toString().length() > 0)
                {
                    args.add("-s");
                    StringBuilder arg = new StringBuilder();
                	for (int i = 0 ; i < sourceDirectories.size(); i++)
                	{
                		if (arg.length() > 0)
                			arg.append(';');
                		arg.append(sourceDirectories.get(i).toString());
                	}
                	args.add(arg.toString());                	
                }
                if (verbose)
                    args.add("-v");

                for (String interfaceName : interfaceClassNames)
                {
                	args.add(interfaceName);
                }

                Jibx2Wsdl.main((String[]) args.toArray(new String[args.size()]));
	        
            } catch (JiBXException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	
	    }
    }
    
    /**
     * Verifies the plugins configuration and sets default values if needed.
     * Note: Remember to call inherited methods first.
     */
    protected void checkConfiguration() {
    	super.checkConfiguration();
    	
        if (this.customizations == null) {
            this.customizations = new ArrayList<String>();
        }
        if (this.options == null) {
            this.options = new HashMap<String, String>();
        }
        if (this.sourceDirectories == null) {
        	this.sourceDirectories = new ArrayList<String>();
        	this.sourceDirectories.add(DEFAULT_SOURCE_DIRECTORY);
        }
    }


}
