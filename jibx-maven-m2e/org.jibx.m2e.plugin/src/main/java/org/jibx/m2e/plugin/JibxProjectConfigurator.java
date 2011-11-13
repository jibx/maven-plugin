/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.jibx.m2e.plugin;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;

public class JibxProjectConfigurator
    extends AbstractJavaProjectConfigurator
{
	public JibxProjectConfigurator()
	{
		super();
	}
    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade,
                                                         MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new JibxBuildParticipant( execution );
    }
    
    @Override
    protected File[] getSourceFolders( ProjectConfigurationRequest request, MojoExecution mojoExecution )
            throws CoreException
        {
    		File[] files = super.getSourceFolders(request, mojoExecution);
    		// todo(don) Figure out why the source folder comes back null.
    		if (files == null)
    			files = new File[0];
    		List<File> list = new Vector<File>();
    		for (File file : files)
    		{
    			if (file != null)
    				list.add(file);
    		}
    		files = new File[0];
    		files = (File[])list.toArray(files);
            return files;
        }

    @Override
    protected String getOutputFolderParameterName()
    {
        return "targetDirectory";
    }
}
