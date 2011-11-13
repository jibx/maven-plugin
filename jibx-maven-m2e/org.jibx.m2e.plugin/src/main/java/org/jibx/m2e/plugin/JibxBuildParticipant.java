/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.jibx.m2e.plugin;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class JibxBuildParticipant
    extends MojoExecutionBuildParticipant
{

    public JibxBuildParticipant( MojoExecution execution )
    {
        super( execution, true );
    }

    @Override
    public Set<IProject> build( int kind, IProgressMonitor monitor )
        throws Exception
    {
        // check if any of the grammar files changed
        if (!anyChanges())
        	return null;

        // execute mojo
        Set<IProject> result = super.build( kind, monitor );

        // tell m2e builder to refresh generated files
        refreshFiles();

        return result;
    }
    
    /**
     * See if there are any changes in the source files.
     * @return
     * @throws Exception
     */
    public boolean anyChanges()
    {
        BuildContext buildContext = getBuildContext();
        
		if ("schema-codegen".equalsIgnoreCase(getMojoExecution().getGoal()))
		{
			File source = getSourceDirectoryFromParam("schemaLocation");
			if (source == null)
				source = getSourceDirectoryFromParam("directory");
			if (source == null)
				source = getDirectoryFromRelativePath("src/main/config");
			if ((source == null) || (!source.exists())) //?? || (!source.isDirectory()))
				return true;	// Probably a url
	        
	        // System.out.println("source scanning " + source);

	        Scanner ds = buildContext.newScanner( source ); // delta or full scanner
		    ds.scan();
		    String[] includedFiles = ds.getIncludedFiles();
		    if (includedFiles == null || includedFiles.length <= 0 )
		        return false;
		}
		if ("bind".equalsIgnoreCase(getMojoExecution().getGoal()))
		{
			File source = getSourceDirectoryFromParam("bindingDirectory");
			if (source == null)
				source = getSourceDirectoryFromParam("directory");
			if (source != null)
				if (source.toString().endsWith("generated-sources"))
					source = null;	// If they put the binding files here, there is no way to sense a change
			if (source == null)
				source = getDirectoryFromRelativePath("src/main/config");
			if ((source == null) || (!source.exists()) || (!source.isDirectory()))
				return true;

			// System.out.println("bind scanning " + source);

		    try {
				Scanner ds = buildContext.newScanner( source ); // delta or full scanner
				ds.scan();
				String[] includedFiles = ds.getIncludedFiles();
				if (includedFiles == null || includedFiles.length <= 0 )
				    return false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
    }
    /**
     * Refresh the built files.
     * @throws Exception
     */
    public void refreshFiles()
            throws Exception
    {
        BuildContext buildContext = getBuildContext();
        
		if ("schema-codegen".equalsIgnoreCase(getMojoExecution().getGoal()))
		{
			File generated = getSourceDirectoryFromParam("targetDirectory");
			if (generated == null)
				generated = getSourceDirectoryFromParam("schemaBindingDirectory");
			if (generated == null)
				generated = getDirectoryFromRelativePath("target/generated-sources");
			if ((generated == null) || (!generated.exists())|| (!generated.isDirectory()))
				return;
			
	        // tell m2e builder to refresh generated files
			// System.out.println("schema-codegen refreshing: " + generated);

			buildContext.refresh( generated );
		}
		if ("bind".equalsIgnoreCase(getMojoExecution().getGoal()))
		{
			File generated = getDirectoryFromRelativePath("target/classes");
			if ((generated == null) || (!generated.exists())|| (!generated.isDirectory()))
				return;

			// System.out.println("bind refreshing: " + generated);

			buildContext.refresh( generated );
		}
    }
	protected File getSourceDirectoryFromParam(String param)
	{
        IMaven maven = MavenPlugin.getMaven();
	    File source = null;
        try {
        	source = maven.getMojoParameterValue(getSession(), getMojoExecution(), param, File.class);
		} catch (CoreException e) {
		}
        return source;
	}
	protected File getDirectoryFromRelativePath(String path)
	{
    	File baseDir = getSession().getCurrentProject().getBasedir();
    	return new File(baseDir, path);
    }
}
