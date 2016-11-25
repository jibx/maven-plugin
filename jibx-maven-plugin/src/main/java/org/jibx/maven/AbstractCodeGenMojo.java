/*
 * Copyright (c) 2004-2005, Dennis M. Sosnoski All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.*;
import java.io.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;

import org.jibx.runtime.JiBXException;
import org.jibx.schema.codegen.CodeGen;

/**
 * Generates Java sources from XSD schemas.
 * @author                        <a href="mailto:jerome.bernard@elastic-grid.com">Jerome Bernard</a>
 * @author                        <a href="mailto:don@tourgeek.com">Don Corley</a>
 */
public abstract class AbstractCodeGenMojo extends AbstractJibxMojo {

    /**
     * Control flag for verbose processing reports.
     *
     * @parameter  expression="${verbose}" default-value="false"
     * @required
     */
    private boolean verbose;

    /**
     * Default package for code generated from schema definitions with no namespace.
     *
     * @parameter
     */
    private String defaultPackage = null;

    /**
     * Include pattern for customization files.
     *
     * @parameter  expression="${customizations}"
     */
    private ArrayList<String> customizations;

    /**
     * The directory or web location which contains XSD files.
     * This can be the schema directory, url, or the base url for a list of
     * &lt;includes&gt; schema (<a href="schema-codegen.html">See example</a>).
     * Defaults to "src/main/config" (or "src/test/config" for test cases).
     *
     * @parameter  expression="${schemaLocation}"
     */
    private String schemaLocation;

    /**
     * The directory which contains XSD files.
     * @deprecated - Since binding and codegen use this for different directories,
     * use schemaLocation.
     * Defaults to "src/main/config" (or "src/test/config" for test cases).
     *
     * @parameter  expression="${directory}"
     */
	@Deprecated
    private String directory;

    /**
     * Namespace applied in code generation when no-namespaced schema definitions are found (to generate
     * no-namespaced schemas as though they were included in a particular namespace)
     *
     * @parameter
     */
    private String defaultNamespace = null;

    /**
     * Exclude pattern for schema files.
     * 
     * @parameter  expression="${excludeSchemas}"
     */
    ArrayList<String> excludeSchemas;

    /**
     * Include pattern for schema files.<br/>
     * <b>Note: </b>Uses the standard filter format described in the plexus
     * <a href="http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/DirectoryScanner.html">DirectoryScanner</a>.<br/>
     * <b>Defaults value is:</b> *.xsd.
     *
     * @parameter  expression="${includeSchemas}"
     */
    ArrayList<String> includeSchemas;

    /**
     * Include pattern for binding files.<br/>
     * @deprecated - This name was confusing, because it is not the list of binding files, but is the list of base
     * binding files. Use includeBaseBindings instead. You will have to change your declaration slightly.
     * From:
     * &lt;includeBindings&gt;<br/>
     * &lt;includeBinding&gt;base-binding.xml&lt;/includeBinding&gt;<br/>
     * &lt;/includeBindings&gt;<br/>
     * To:
     * &lt;includeBaseBindings&gt;<br/>
     * &lt;includeBaseBinding&gt;<br/>
     * &lt;includeBindings&gt;<br/>
     * &lt;includeBinding&gt;base-binding.xml&lt;/includeBinding&gt;<br/>
     * &lt;/includeBindings&gt;<br/>
     * &lt;/includeBaseBinding&gt;<br/>
     * &lt;/includeBaseBindings&gt;<br/>
     *
     * @parameter  expression="${includeBindings}"
     */
    @Deprecated
    ArrayList<String> includeBindings;

    /**
     * Target directory where to generate Java source files and the binding file.
     * @deprecated - This param is now the same as in the bind goal; change it to schemaBindingDirectory.
     * Defaults to "target/generated-sources" (or "target/generated-test-sources" for test cases)
     *
     * @parameter  expression="${targetDirectory}"
     */
    @Deprecated
    private String targetDirectory;

    /**
     * Extra options to be given for customization via CLI.<p/>
     * Enter extra customizations or other command-line options.<br/>
     * The extra customizations are described on the 
     * <a href="/fromschema/codegen-customs.html">CodeGen customizations page</a><br/>
     * The single character CodeGen commands may also be supplied here.<br/>
     * For example, to include a base binding file (-i) and prefer-inline code, supply the following options:<br/>
     * <code>
     * &lt;options&gt;<br/>
     * &nbsp;&nbsp;&lt;i&gt;base-binding.xml&lt;/i&gt;<br/>
     * &nbsp;&nbsp;&lt;prefer-inline&gt;true&lt;/prefer-inline&gt;<br/>
     * &lt;/options&gt;
     * </code>
     *
     * @parameter
     */
    private Map<String,String> options;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        checkConfiguration();

        if ("pom".equalsIgnoreCase(project.getPackaging()))
        {
            getLog().info("Not running JiBX code generator for pom packaging");
        	return;		// Don't bind jibx if pom packaging
        }

        List<String> args = new ArrayList<String>();
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
        if (verbose)
            args.add("-v");
        if (defaultPackage != null) {
            args.add("-n");
            args.add(defaultPackage);
        }
        args.add("-t");
        args.add(getFullPath(getSchemaBindingDirectory()));
        if (customizations.size() > 0) {
            args.add("-c");
            for (String customization : customizations) {
                args.add(customization);
            }
        }
        if (defaultNamespace != null) {
            args.add("-u");
            args.add(defaultNamespace);
        }

        String allBindings = "";
        String mode;
        String[] bindings = new String[0];
        String[] classpaths;
        if (isMultiModuleMode()) {
            if (isRestrictedMultiModuleMode()) {
                mode = "restricted multi-module";
            } else {
                mode = "multi-module";
            }
            classpaths = getMultiModuleClasspaths();
        } else {
            mode = "single-module";
            classpaths = getSingleModuleClasspaths();
        }

    	bindings = getBaseBindings(bindings);	// Based bindings
    	for (String binding : bindings) {
	        if (allBindings.length() > 0)
	        	allBindings = allBindings + ",";
	    	allBindings = allBindings + binding;
    	}
        if (allBindings.length() > 0)
        {
        	args.add("-i");
        	args.add(allBindings);
        }
        
        List<String> schemas = getSchemas(getFullPath(getSchemaLocation()));
        for (String schema  : schemas) {
            File file = new File(schema);
            if (file.exists())
            	args.add(new File(schema).toURI().toString());
            else
            {	// Not a file, try a URL
            	try {
					args.add(new URL(schema).toURI().toString());
				} catch (URISyntaxException e) {
					getLog().warn("Target schema is not a valid file or URL - Passing location as is");
					args.add(schema);
				} catch (MalformedURLException e) {
					getLog().warn("Target schema is not a valid file or URL - Passing location as is");
					args.add(schema);
				}
            }
        }
        
        getLog().debug("Adding " + getSchemaBindingDirectory() + " as source directory...");
        project.addCompileSourceRoot(getFullPath(getSchemaBindingDirectory()));

        try {
            getLog().info("Generating Java sources in " + getSchemaBindingDirectory() + " from schemas available in " + getSchemaLocation() + "...");
            CodeGen.main((String[]) args.toArray(new String[args.size()]));
        } catch (Exception e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            throw new MojoExecutionException(cause.getLocalizedMessage(), cause);
        }
    }
    /**
     * Get the binding path name for a single module binding.
     * @param basedir Basedir
     * @param includeBinding Include bindings
     * @return Path
     */
	public String getSingleModuleBindingPath(String basedir, String includeBinding)
	{
        if (!includeBinding.contains(","))
        {	// Possible relative path
        	File file = new File(includeBinding);
        	if (!file.isAbsolute())
            {	// Possible relative path
        		try {
                    file = new File(addToPath(basedir, includeBinding));
                    if (file.exists())
                    	includeBinding = addToPath(basedir, includeBinding);
                } catch (Exception e) {
                	// Exception = use relative path
                }
            }
        }
        return includeBinding;
	}
    /**
     * Verifies the plugins configuration and sets default values if needed.
     * Note: Remember to call inherited methods first.
     */
    protected void checkConfiguration() {
    	super.checkConfiguration();
    	
    	if ((this.includeSchemas == null) || (this.includeSchemas.size() == 0))
            this.includeSchemas = new ArrayList<String>();

        if (this.excludeSchemas == null)
            this.excludeSchemas = new ArrayList<String>();

        if ((this.includes != null) && (this.includes.size() > 0))
    		this.includeSchemas.addAll(this.includes);
    	if ((this.excludes != null) && (this.excludes.size() > 0))
    		this.excludeSchemas.addAll(this.excludes);
    	if (this.includeSchemas.size() == 0) {
            this.includeSchemas.add(DEFAULT_INCLUDES);
        }

    	if (this.customizations == null) {
            this.customizations = new ArrayList<String>();
        }

    	if (this.includeBindings != null)
    			if (this.includeBindings.size() > 0) {
    		IncludeBaseBinding includeBaseBinding = new IncludeBaseBinding();
    		includeBaseBinding.setIncludes(includeBindings);
    		this.includeBaseBindings.add(includeBaseBinding);
        }
    	
        if (this.options == null) {
            this.options = new HashMap<String,String>();
        }
    }

    /**
     * Returns all bindings in the given directory according to the configured include/exclude patterns.
     */
    private List<String> getSchemas(String path) throws MojoExecutionException, MojoFailureException {
        return this.getIncludedFiles(path, this.includeSchemas, this.excludeSchemas);
    }

    /**
     * Get the schema files directory.
     * @return The binding files directory.
     */
    protected String getSchemaLocation()
    {
    	if (schemaLocation != null)
    		return schemaLocation;
    	if (directory != null)
        	return directory;
		return getDefaultSchemaLocation();
    }

    /**
     * Get the schema files directory.
     * @return The binding files directory.
     */
    abstract String getDefaultSchemaLocation();

    /**
     * Get the binding files directory.
     * @return The binding files directory.
     */
    protected String getSchemaBindingDirectory()
    {
    	if (schemaBindingDirectory != null)
    		return schemaBindingDirectory;
    	if (targetDirectory != null)
    		return targetDirectory;
    	return super.getSchemaBindingDirectory();	// This is not used for code-gen, but the binding compiler should be looking here
    }

}