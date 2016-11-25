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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Runs the JiBX binding compiler.
 *
 * @author                        <a href="mailto:mail@andreasbrenk.com">Andreas Brenk</a>
 * @author                        <a href="mailto:frankm.os@gmail.gom">Frank Mena</a>
 * @author                        <a href="mailto:don@tourgeek.com">Don Corley</a>
 */
public abstract class AbstractJibxMojo extends AbstractMojo {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    static final String DEFAULT_INCLUDE_BINDINGS = "binding.xml";
    static final String DEFAULT_INCLUDES = "*.xsd";

    /**
     * The maven project.
     *
     * @resolveTransitiveDependencies
     * @parameter                      expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * A list of modules to search for binding files in the format: groupID:artifactID
     *
     * @parameter  expression="${modules}"
     */
    HashSet<String> modules;

    /**
     * A list of modules or files to search for base binding files.
     * This can specify files in the local directory or files stored in your dependencies.
     *
     * If your based binding files are in a local file system, specify them as follows:<br/>
     * &lt;includeBaseBindings&gt;<br/>
     * &nbsp;&nbsp;&lt;includeBaseBinding&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;directory&gt;src/main/config&lt;/directory&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;includes&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;include&gt;base-binding.xml&lt;/include&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/includes&gt;<br/>
     * &nbsp;&nbsp;&lt;/includeBaseBinding&gt;<br/>
     * &nbsp;&lt;/includeBaseBindings&gt;<br/>
     * 
     * If your based binding files are in a artifact that is one of your dependencies:<br/>
     * &lt;includeBaseBindings&gt;<br/>
     * &nbsp;&nbsp;&lt;includeBaseBinding&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;com.mycompany.baseschema&lt;/groupId&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;base-schema&lt;/artifactId&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;classifier&gt;bindings&lt;/classifier&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;directory&gt;META-INF&lt;/directory&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;includes&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;include&gt;base-binding.xml&lt;/include&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/includes&gt;<br/>
     * &nbsp;&nbsp;&lt;/includeBaseBinding&gt;<br/>
     * &lt;/includeBaseBindings&gt;<br/>
     * 
     * The classifier is optional (if your binding file is not in the main artifact)
     * A version is not necessary, since this declaration must be on your list of dependencies.<br/>
     * <b>Note: </b>For file filters, use the standard filter format described in the plexus
     * <a href="http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/DirectoryScanner.html">DirectoryScanner</a>.<br/>
     * <b>Defaults value is:</b> binding.xml.
     * Include existing bindings and use mappings from the bindings for matching schema global definitions.
     * (this is the basis for modular code generation)<br/>
     * <b>Note:</b> If directory is not specified, relative paths start at &lt;baseBindingDirectory&gt;.
     * 
     * @parameter  expression="${includeBaseBindings}"
     */
    HashSet<IncludeBaseBinding> includeBaseBindings;

    /**
     * Control flag multi-module mode.
     *
     * @parameter  expression="${multi-module}" default-value="false"
     * @required
     */
    boolean multimodule;

    /**
     * Exclude pattern for binding files.
     * @deprecated - This name was confusing since it is used as a binding file pattern
     * for bind and a schema file pattern for code-gen.<br/>
     * Use excludeSchemaBindings for binding and excludeSchemas for schema
     * 
     * @parameter  expression="${excludes}"
     */
    @Deprecated
    ArrayList<String> excludes;

    /**
     * Include pattern for binding files.<br/>
     * @deprecated - This name was confusing since it is used as a binding file pattern
     * for bind and a schema file pattern for code-gen.<br/>
     * Use includeSchemaBindings for binding and includeSchemas for schema.<br/>
     * <b>Note: </b>Uses the standard filter format described in the plexus
     * <a href="http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/DirectoryScanner.html">DirectoryScanner</a>.<br/>
     * <b>Defaults value is:</b> binding.xml.
     *
     * @parameter  expression="${includes}"
     */
    @Deprecated
    ArrayList<String> includes;

    /**
     * The directory which contains schema binding files.
     * Defaults to "src/main/config" (or "src/test/config" for test goals).
     * For code-gen or if the default directory does not exist, defaults to
     * "target/generated-sources" (or "target/generated-test-sources" for test goals).
     *
     * @parameter  expression="${schemaBindingDirectory}"
     */
    String schemaBindingDirectory;

    /**
     * Exclude pattern for schema binding files.
     * 
     * @parameter  expression="${excludeSchemaBindings}"
     */
    ArrayList<String> excludeSchemaBindings;

    /**
     * Include pattern for schema binding files.<br/>
     * <b>Note: </b>Uses the standard filter format described in the plexus
     * <a href="http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/DirectoryScanner.html">DirectoryScanner</a>.<br/>
     * <b>Defaults value is:</b> binding.xml.
     * Include existing bindings and use mappings from the bindings for matching schema global definitions.
     * (this is the basis for modular code generation)
     * Include base bindings as follows:<br/>
     * &lt;includeSchemaBindings&gt;<br/>
     * &nbsp;&nbsp;&lt;includeSchemaBinding&gt;base-binding.xml&lt;/includeSchemaBinding&gt;<br/>
     * &lt;/includeSchemaBindings&gt;<br/>
     * <b>Note:</b> Relative paths start at &lt;directory&gt;.
     *
     * @parameter  expression="${includeSchemaBindings}"
     */
    ArrayList<String> includeSchemaBindings;

    /**
     * Get the default location of the base binding files.<br/>
     * <b>Defaults value is:</b> schemaBindingDirectory.
     *
     * @parameter  expression="${baseBindingDirectory}"
     */
    String baseBindingDirectory;

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Determines if running in single- or multi-module mode, collects all bindings and finally runs the binding
     * compiler.
     */
    public abstract void execute() throws MojoExecutionException, MojoFailureException;

    /**
     * Returns the basedir of the given project.
     *
     * @param project Project
     * @return basedir of the given project
     */
    protected String getProjectBasedir(MavenProject project) {
        return FilenameUtils.normalize(project.getBasedir().getAbsolutePath());
    }

    /**
     * Verifies the plugins configuration and sets default values if needed.
     * Note: Remember to call inherited methods first.
     */
    protected void checkConfiguration() {
    	
    	if (this.includeSchemaBindings == null) {
            this.includeSchemaBindings = new ArrayList<String>();
        }
        if (this.excludeSchemaBindings == null) {
            this.excludeSchemaBindings = new ArrayList<String>();
        }

        if ((this.modules != null) && (this.modules.size() > 0)) {
            this.multimodule = true;
        } else {
            this.modules = null;
        }
        
        if (this.includeBaseBindings == null)
        	this.includeBaseBindings = new HashSet<IncludeBaseBinding>();
    }
    
    /**
     * Returns all bindings in the given directory according to the configured include/exclude patterns.
     *
     * @param excludeFiles Files to exclude
     * @param includeFiles Files to include
     * @param path Path
     * @return List of included files
     * @throws MojoExecutionException error
     * @throws MojoFailureException error
     */
    protected List<String> getIncludedFiles(String path, ArrayList<String> includeFiles, ArrayList<String> excludeFiles) throws MojoExecutionException, MojoFailureException {
        List<String> bindingSet = new ArrayList<String>();

        File bindingdir = new File(path);
        if (!bindingdir.exists())
        {	// Probably a url...
        	try {
				URL url = new URL(path);
				if ("file".equalsIgnoreCase(url.getProtocol()))
					return bindingSet;	// If you pass a file or directory it must exist, so I can scan it; return empty dir
			} catch (MalformedURLException e) {
				return bindingSet;
			}
			String[] includes = (String[]) includeFiles.toArray(new String[includeFiles.size()]);
			if ((includes != null) && (includes .length > 0))
			{
				if (includes.length == 1)
					if (DEFAULT_INCLUDES == includes[0])
						if ((!path.endsWith("/")) && (!path.endsWith(File.separator)))
						{
							bindingSet.add(path);
							return bindingSet;	// Special case, user supplied complete URL as schemaLocation
						}
				for (String include : includes) {
					bindingSet.add(path + include);
				}
			}
			else
				bindingSet.add(path);	// Only supplied a single URL
        }
        if (!bindingdir.isDirectory()) {
            return bindingSet;
        }

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(bindingdir);
        String[] includes = (String[]) includeFiles.toArray(new String[includeFiles.size()]);
        scanner.setIncludes(includes);
        String[] excludes = (String[]) excludeFiles.toArray(new String[excludeFiles.size()]);
        scanner.setExcludes(excludes);
        scanner.scan();

        String[] files = scanner.getIncludedFiles();
        String absolutePath = bindingdir.getAbsolutePath();
        for (int i = 0; i < files.length; i++) {
            String file = absolutePath + File.separator + files[i];
            bindingSet.add(file);
        }

        return bindingSet;
    }
    /**
     * Fix this file path.
     * If it is absolute, leave it alone, if it is relative prepend the default path or the base path.
     * @param filePath The path to fix
     * @param defaultPath The base path to use.
     * @return The absolute path to this file.
     */
    protected String fixFilePath(String filePath, String defaultPath)
    {
    	if (filePath == null)
    		return null;
        boolean relativePath = true;
        
        String basedir = defaultPath;
        if (basedir == null)
        	basedir = getProjectBasedir(this.project);
    	File file = new File(filePath);
    	if (!file.isAbsolute())
        {	// Possible relative path
    		try {
                file = new File(addToPath(basedir, filePath));
                if (file.exists())
                	filePath = addToPath(basedir, filePath);
            } catch (Exception e) {
            	// Exception = use relative path
            }
        }
        return filePath;
    }

    /**
     * Normalizes all entries.
     */
    /* package */ String[] normalizeClasspaths(Set<String> classpathSet) {
        String[] classpaths = (String[]) classpathSet.toArray(new String[classpathSet.size()]);

        for (int i = 0; i < classpaths.length; i++) {
            classpaths[i] = FilenameUtils.normalize(classpaths[i]);
        }

        return classpaths;
    }

    /**
     * Returns all bindings in the given directory according to the configured include/exclude patterns.
     * @throws MojoExecutionException error
     * @throws MojoFailureException error
     */
    List<String> getBindings(String path) throws MojoExecutionException, MojoFailureException {
        return this.getIncludedFiles(path, this.includeSchemaBindings, this.excludeSchemaBindings);
    }

    /**
     * Returns all binding in the current project and all referenced projects (multi-module mode)
     * @throws MojoExecutionException error
     * @throws MojoFailureException error
     */
    String[] getMultiModuleBindings() throws MojoExecutionException, MojoFailureException {
        Set<String> basedirSet = getProjectBasedirSet(this.project);
        Set<String> bindingSet = new HashSet<String>();
        // No Need to add project (single module mode) dir, it is included in the baseDirSet

        for (String basedir : basedirSet) {
            if (basedir.equals(getProjectBasedir(this.project)))
            	basedir = getFullPath(getSchemaBindingDirectory());	// Main project
            else
            	basedir = addToPath(basedir, getSchemaBindingDirectory());
            List<String> bindingList = getBindings(basedir);
            bindingSet.addAll(bindingList);
        }

        return (String[]) bindingSet.toArray(new String[bindingSet.size()]);
    }

    /**
     * Returns the classpath for the binding compiler running in multi-module mode.
     * @throws MojoExecutionException error
     * @throws MojoFailureException error
     */
    String[] getMultiModuleClasspaths() throws MojoExecutionException, MojoFailureException {
        Set<String> classpathSet = getProjectCompileClasspathElementsSet(this.project);

        return normalizeClasspaths(classpathSet);
    }

    /**
     * Returns the basedir of the given project and all (or all in "modules" specified) reference projects.
     */
    @SuppressWarnings("unchecked")
	private Set<String> getProjectBasedirSet(MavenProject project) {
        Set<String> basedirSet = new HashSet<String>();
        basedirSet.add(getProjectBasedir(project));

        for (MavenProject projectReference : (Collection<MavenProject>)project.getProjectReferences().values()) {
            String projectId = projectReference.getGroupId() + ":" + projectReference.getArtifactId();

            if ((this.modules == null) || this.modules.contains(projectId)) {
                basedirSet.add(getProjectBasedir(projectReference));
            }
        }

        return basedirSet;
    }

    /**
     * Returns the build output directory of the given project.
     *
     * @throws  MojoExecutionException  if DependencyResolutionRequiredException occurs
     * @param project Project
     * @return Output directory
     */
    protected abstract Set<String> getProjectCompileClasspathElements(MavenProject project) throws MojoExecutionException;

    /**
     * Returns the build output directory of the given project and all its reference projects.
     *
     * @param project Project
     * @return Output directory
     * @throws  MojoExecutionException  if DependencyResolutionRequiredException occurs
     */
    @SuppressWarnings("unchecked")
	private Set<String> getProjectCompileClasspathElementsSet(MavenProject project) throws MojoExecutionException {
        Set<String> classpathElements = new HashSet<String>();
        classpathElements.addAll(getProjectCompileClasspathElements(project));

        for (MavenProject projectReference : (Collection<MavenProject>)project.getProjectReferences().values()) {
            classpathElements.addAll(getProjectCompileClasspathElements(projectReference));        	
        }

        return classpathElements;
    }

    /**
     * Returns all bindings in the current project (single-module mode).
     * @return bindings
     * @throws MojoExecutionException error
     * @throws MojoFailureException error
     */
    String[] getSingleModuleBindings() throws MojoExecutionException, MojoFailureException {
        String bindingdir = getFullPath(getSchemaBindingDirectory());
        List<String> bindingSet = getBindings(bindingdir);

        return (String[]) bindingSet.toArray(new String[bindingSet.size()]);
    }

    /**
     * Returns the classpath for the binding compiler running in single-module mode.

     * @return classpaths
     * @throws MojoExecutionException error
     * @throws MojoFailureException error
     */
    String[] getSingleModuleClasspaths() throws MojoExecutionException, MojoFailureException {
        Set<String> classpathSet = getProjectCompileClasspathElements(this.project);

        return normalizeClasspaths(classpathSet);
    }

    /**
     * Get the binding path name for a includeBaseBinding binding.
     * This method actually unjars the binding file(s) from dependent resources.
     * @return bindings
     */
	public String[] getBaseBindings(String[] bindings)
	{
    	List<String> bindingSet = new ArrayList<String>();
    	for (String binding : bindings) {
    		bindingSet.add(binding);
    	}
        if (includeBaseBindings.size() > 0)
    	{
	        for (IncludeBaseBinding includeDependentBinding : includeBaseBindings)
	        {
	            if (includeDependentBinding == null)
	            	continue;
				if ((includeDependentBinding.groupId == null ) || (includeDependentBinding.artifactId == null))
					bindingSet = addManualDependentBinding(includeDependentBinding, bindingSet);	// Probably a reference to a binding on the filesystem
				else
					bindingSet = addDependentBinding(includeDependentBinding, bindingSet);
	        }
        }

        return bindingSet.toArray(new String[bindingSet.size()]);
	}
	/**
	 * Add bindings on the filesystem.
	 * @param includeDependentBinding dependent binding
	 * @param bindingSet List of bindings
	 * @return bindings
	 */
	public List<String> addManualDependentBinding(IncludeBaseBinding includeDependentBinding, List<String> bindingSet)
	{
		String directory = includeDependentBinding.directory;
		if (directory == null)
			directory = this.getBaseBindingDirectory();
		String path = fixFilePath(directory, null);
        File bindingdir = new File(path);
        if (!bindingdir.exists())
        {	// Probably a url...
        	try {
				URL url = new URL(path);
				if (!"file".equalsIgnoreCase(url.getProtocol()))
				{
					String[] includes = (String[]) includeDependentBinding.includes.toArray(new String[includeDependentBinding.includes.size()]);
					if ((includes != null) && (includes .length > 0))
					{
						for (String include : includes) {
							bindingSet.add(path + include);
						}
					}
					else
						bindingSet.add(path);	// Only supplied a single URL
				}
				return bindingSet;
			} catch (MalformedURLException e) {
				// Try as a file
			}
        }

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(bindingdir);
        if (includeDependentBinding.includes.size() == 1)
        	if (includeDependentBinding.includes.get(0) == null)
        		includeDependentBinding.includes.remove(0);	// Sometime when using params a null include is passed, so skip it.
        String[] includes = (String[]) includeDependentBinding.includes.toArray(new String[includeDependentBinding.includes.size()]);
        scanner.setIncludes(includes);
        //String[] excludes = (String[]) excludes.toArray(new String[excludeFiles.size()]);
        //scanner.setExcludes(excludes);
        for (String include : includes)
        {
        	File file = new File(include);
        	if (file.isAbsolute())
        	{	// If the files specify an absolute path, don't scan
		        for (String includez : includes)
		        {
		            bindingSet.add(includez);
		        }
		        return bindingSet;
        	}
        }
        if (!bindingdir.isDirectory()) {
            return bindingSet;
        }
        scanner.scan();

        String[] files = scanner.getIncludedFiles();
        String absolutePath = bindingdir.getAbsolutePath();
        for (int i = 0; i < files.length; i++) {
            String file = addToPath(absolutePath, files[i]);
            bindingSet.add(file);
        }
        return bindingSet;
	}
	/**
	 * Add bindings that are containted in artifacts.
     * @param includeDependentBinding dependent binding
     * @param bindingSet List of bindings
     * @return bindings
	 */
	public List<String> addDependentBinding(IncludeBaseBinding includeDependentBinding, List<String> bindingSet)
	{
		@SuppressWarnings("unchecked")
		Set<Artifact> artifacts = this.project.getArtifacts();
        for (Artifact artifact : artifacts)
        {
        	if ((!"jar".equals(artifact.getType()))
        			&& (!"bundle".equals(artifact.getType()))
        			&& (!"zip".equals(artifact.getType())))
        		continue;
        	if ((!includeDependentBinding.groupId.equals(artifact.getGroupId()))
        			|| (!includeDependentBinding.artifactId.equals(artifact.getArtifactId())))
        		continue;
        	if (includeDependentBinding.classifier != null)
        		if (!includeDependentBinding.classifier.equals(artifact.getClassifier()))
        			continue;
        	bindingSet = addDependentArtifact(artifact, includeDependentBinding, bindingSet);
			
        }
        return bindingSet;
	}
    public List<String> addDependentArtifact(Artifact artifact, IncludeBaseBinding includeDependentBinding, List<String> bindingSet)
    {
    	// Found it! Go through this jar and see if these binding files exist
		ZipFile zip = null;
		try {
			zip = new ZipFile(artifact.getFile());
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if ((includeDependentBinding.includes == null) || (includeDependentBinding.includes.size() == 0))
			bindingSet = extractBaseBindingFile(bindingSet, zip, includeDependentBinding.directory, includeDependentBinding.groupId, includeDependentBinding.artifactId, DEFAULT_INCLUDE_BINDINGS);
		else
		{
			for (String include : includeDependentBinding.includes) {
				bindingSet = extractBaseBindingFile(bindingSet, zip, includeDependentBinding.directory, includeDependentBinding.groupId, includeDependentBinding.artifactId, include);
			}
		}
		// TODO(don) I can't figure out how to read the project info, so for now, I'll just get all artifacts from dependencies of this artifact
		@SuppressWarnings("unchecked")
		Set<Artifact> artifacts = this.project.getArtifacts();
    	List<String> artifactTrails = artifact.getDependencyTrail();
        for (Artifact artifact2 : artifacts)
        {
        	List<String> dependencyTrails = artifact2.getDependencyTrail();
        	boolean match = true;
        	int i = 0;
        	for (; i < artifactTrails.size(); i++)
        	{
        		if (dependencyTrails.size() <= i)
        			match = false;
        		else if (!dependencyTrails.get(i).equals(artifactTrails.get(i)))
        			match = false;
        	}
    		if (match)
        		if (dependencyTrails.size() > i)
    		{
        		IncludeBaseBinding includeBaseBinding2 = new IncludeBaseBinding();
        		includeBaseBinding2.setArtifactId(artifact2.getArtifactId());
        		includeBaseBinding2.setClassifier(artifact2.getClassifier());
        		includeBaseBinding2.setDirectory(includeDependentBinding.directory);
        		includeBaseBinding2.setGroupId(artifact2.getGroupId());
        		includeBaseBinding2.setIncludes(includeDependentBinding.includes);
        		includeBaseBinding2.setVersion(artifact2.getVersion());
        		bindingSet = addDependentArtifact(artifact2, includeBaseBinding2, bindingSet);
    		}
        }
		return bindingSet;
    }
	/**
	 * Extract the base binding file(s) from the dependent artifacts.
	 * @param pathList pathList
	 * @param zip zip
	 * @param directory directory
	 * @param groupId group
	 * @param artifactId artifact
	 * @param filePathInJar file path
	 * @return bindings
	 */
	public List<String> extractBaseBindingFile(List<String> pathList, ZipFile zip, String directory, String groupId, String artifactId, String filePathInJar)
	{
		try {
			String filenameIn = filePathInJar;
			if (directory != null)
				filenameIn = addToPath(directory, filenameIn);
			ZipEntry entry = zip.getEntry(filenameIn);
			filenameIn = filePathInJar;
			int lastSlash = filenameIn.lastIndexOf(File.separator);
			if (lastSlash == -1)
				lastSlash = filenameIn.lastIndexOf('/');
			String startFilePath = "";
			if (lastSlash != -1)
			{
				startFilePath = File.separator + filenameIn.substring(0, lastSlash);
				filenameIn = filenameIn.substring(lastSlash + 1);
			}
			if (entry != null)
			{
				String filenameOut = getFullPath(getBaseBindingDirectory()) + startFilePath;
				new File(filenameOut).mkdirs();
				filenameOut = filenameOut + File.separator + groupId + '-' + artifactId + '-' + filenameIn;
				File fileOut = new File(filenameOut);
				if (fileOut.exists())
					fileOut.delete();	// Hmmmmm - optimize LATER!
				InputStream inStream = zip.getInputStream(entry);
				if (fileOut.createNewFile())
				{
					OutputStream outStream = new FileOutputStream(filenameOut);
					byte[] b = new byte[1000];
					int size = 0;
					while ((size = inStream.read(b)) > 0)
					{
						outStream.write(b, 0, size);
					}
					outStream.close();
					inStream.close();
					pathList.add(filenameOut);
				}
			}
		
	} catch (IOException e) {
		e.printStackTrace();
	}
	return pathList;
}
    /**
     * Determine if the plugin is running in "multi-module" mode.
     */
    boolean isMultiModuleMode() {
        return this.multimodule;
    }

    /**
     * Determine if the plugin is running in "restricted multi-module" mode.
     */
    boolean isRestrictedMultiModuleMode() {
        return isMultiModuleMode() && (this.modules != null);
    }

    /**
     * Determine if the plugin is running in "single-module" mode.
     */
    boolean isSingleModuleMode() {
        return !isMultiModuleMode();
    }
    
    /**
     * Get the base binding files directory.
     * @return The binding files directory.
     */
    protected String getBaseBindingDirectory()
    {
    	if (baseBindingDirectory != null)
    		if (baseBindingDirectory.length() > 0)
    			return baseBindingDirectory;
    	return getSchemaBindingDirectory();
    }
    
    /**
     * Get the binding files directory.
     * @return The binding files directory.
     */
    protected String getSchemaBindingDirectory()
    {
    	if (schemaBindingDirectory != null)
    		return schemaBindingDirectory;
    	return getDefaultSchemaBindingDirectory();
    }

    /**
     * Get the binding files directory.
     * @return The binding files directory.
     */
    abstract String getDefaultSchemaBindingDirectory();

    /**
     * Fix this directory path so it starts at the root project dir.
     * @param dir directory
     * @return file path
     */
    public String getFullPath(String dir)
    {
    	try {
			URL url = new URL(dir);
			if (!"file".equalsIgnoreCase(url.getProtocol()))
				return dir;	// If you pass a valid non file URL, use it!
		} catch (MalformedURLException e) {
		}
		File file = new File(dir);
		if (!file.isAbsolute())
    		dir = addToPath(getProjectBasedir(project), dir);
    	return dir;
    }
    /**
     * Fix this directory path so it starts at the root project dir.
     * @param prePath path
     * @param postPath post path
     * @return complete path
     */
    public String addToPath(String prePath, String postPath)
    {
    	if ((prePath.length() > 0) && (!prePath.endsWith(File.separator))
    		&& (postPath.length() > 0) && (!postPath.startsWith(File.separator)))
    		return prePath + File.separator + postPath;
    	else
    		return prePath + postPath;
    }
}
