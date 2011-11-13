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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jibx.binding.Compile;
import org.jibx.runtime.JiBXException;

/**
 * Runs the JiBX binding compiler.
 *
 * @author                        <a href="mailto:mail@andreasbrenk.com">Andreas Brenk</a>
 * @author                        <a href="mailto:frankm.os@gmail.gom">Frank Mena</a>
 * @author                        <a href="mailto:don@tourgeek.com">Don Corley</a>
 */
public abstract class AbstractBaseBindingMojo extends AbstractJibxMojo {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /**
     * Control flag for test loading generated/modified classes.
     *
     * @parameter  expression="${load}" default-value="false"
     * @required
     */
    boolean load;

    /**
     * Control flag for test loading generated/modified classes.
     *
     * @parameter  expression="${validate}" default-value="true"
     * @required
     */
    boolean validate;

    /**
     * Control flag for verbose processing reports.
     *
     * @parameter  expression="${verbose}" default-value="false"
     * @required
     */
    boolean verbose;

    /**
     * Control flag for verifying generated/modified classes with BCEL.
     *
     * @parameter  expression="${verify}" default-value="false"
     * @required
     */
    boolean verify;

    /**
     * The directory which contains binding files.
     * @deprecated - Since binding and codegen use this for different locations,
     * use schemaBindingDirectory.<br/>
     * Defaults to "src/main/config" (or "src/test/config" for test goals).
     *
     * @parameter  expression="${directory}"
     */
	@Deprecated
    private String directory;

    //~ Methods --------------------------------------------------------------------------------------------------------

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
        getBaseBindings(bindings);	// This is not a mistake. I call getDependentBindingPaths to unzip the archives
        	// But I do not add the bindings to my list (since they are base binding.. already included in my binding file)

        if (bindings.length == 0) {
            getLog().info("Not running JiBX binding compiler (" + mode + " mode) - no binding files");
        } else {
            getLog().info("Running JiBX binding compiler (" + mode + " mode) on " + bindings.length
                          + " binding file(s)");
            compile(classpaths, bindings);
        }
    }

    /**
     * Verifies the plugins configuration and sets default values if needed.
     * Note: Remember to call inherited methods first.
     */
    protected void checkConfiguration() {
    	super.checkConfiguration();

    	if ((this.includes != null) && (this.includes.size() > 0))
    		this.includeSchemaBindings.addAll(this.includes);
    	if ((this.excludes != null) && (this.excludes.size() > 0))
    		this.excludeSchemaBindings.addAll(this.excludes);
    	
    	if ((excludeSchemaBindings.size() == 0) && (includeSchemaBindings.size() == 0))
    		includeSchemaBindings.add(DEFAULT_INCLUDE_BINDINGS);
    }

    /**
     * Creates and runs the JiBX binding compiler.
     */
    private void compile(String[] classpaths, String[] bindings) throws MojoExecutionException {
        try {
            Compile compiler = new Compile();
            compiler.setLoad(this.load);
            compiler.setSkipValidate(!this.validate);
            compiler.setVerbose(this.verbose);
            compiler.setVerify(this.verify);
            compiler.compile(classpaths, bindings);
        } catch (JiBXException e) {
            Throwable cause = (e.getRootCause() != null) ? e.getRootCause() : e;
            throw new MojoExecutionException(cause.getLocalizedMessage(), cause);
        }
    }

    /**
     * Get the binding files directory.
     * @return The binding files directory.
     */
    @Override
    protected String getSchemaBindingDirectory()
    {
    	if (schemaBindingDirectory != null)
    		return schemaBindingDirectory;
    	if (directory != null)
    		return directory;
    	return super.getSchemaBindingDirectory();
    }

}
