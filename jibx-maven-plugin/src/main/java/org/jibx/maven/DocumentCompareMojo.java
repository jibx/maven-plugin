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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jibx.extras.DocumentComparator;
import org.jibx.extras.TestMultRoundtrip;
import org.jibx.extras.TestRoundtrip;
import org.jibx.runtime.JiBXException;
import org.xmlpull.v1.XmlPullParserException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Compares two schemas or roundtrips a schema through a JiBX class and compares the results.
 * If a mapped class is supplied, then the inFile is marshalled into the supplied class and then
 * unmarshalled in the outFile (defaults to temp.xml) and compared with the original xml document.
 * If no class is supplied, then the inFile is compared to the outFile XML files.
 * <b>Note:</b> This mojo only runs in test scope.
 *
 * @author                        <a href="mailto:don@tourgeek.com">Don Corley</a>
 * @goal                          document-compare
 * @phase                         test
 */
public class DocumentCompareMojo extends AbstractJibxMojo {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /**
     * Root class name to use to round-trip the document.
     *
     * @parameter  expression="${mappedClass}"
     */
    private String mappedClass;

    /**
     * The path to the source XML document to compare.
     * NOTE: Relative paths start at ${basedir}.
     *
     * @parameter  expression="${inFile}"
     * @required
     */
    private String inFile;

    /**
     * The path to the destination XML document to compare.
     * NOTE: Relative paths start at ${basedir}; If this path is a filename, the directory is the same as inFile.
     *
     * @parameter  expression="${outFile}"
     */
    private String outFile;

    /**
     * The directory which contains XML files.
     *
     * @parameter  expression="${xmlDirectory}" default-value="src/test/config"
     * @required
     */
    private String xmlDirectory;

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Run the comparison.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkConfiguration();

        if ("pom".equalsIgnoreCase(project.getPackaging()))
        {
            getLog().info("JiBX document compare does not run for pom packaging");
        	return;		// Don't run if pom packaging
        }

        inFile = this.fixFilePath(inFile, null);
        if (outFile != null)
        {
            String defaultOutPath = null;
            if ((!outFile.contains(File.pathSeparator))
                && (!outFile.contains("/")))
            {	// If the outFile has no path info, put it in the same directory as inFile
            	File fileIn = new File(inFile);
            	defaultOutPath = fileIn.getParent();
            }
        	outFile = this.fixFilePath(outFile, defaultOutPath);
        }

        if (mappedClass != null)
        {	// Run the TestRoundtrip
        	ClassLoader parent = this.addTestClasspath();
            
            try {
                if (outFile == null)
                	outFile = inFile;
                if (!MyTestRoundtrip.runTest(mappedClass, null, inFile, outFile)) {
		            throw new MojoExecutionException("Class did not round-trip document on document-compare");	            	
                }
	        } catch (Exception e) {
	            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
	            throw new MojoExecutionException(cause.getLocalizedMessage(), cause);
	        } finally {	
	        	Thread.currentThread().setContextClassLoader(parent);	// Restore
	        }
        }
        else
        {	// Run the DocumentComparator
            if (outFile == null)
            {
                getLog().info("For JiBX document compare you must supply two documents or a class and a document");
            	return;	
            }
            try {
            	File fileIn = new File(inFile);
            	File fileOut = new File(outFile);
                // compare with output document to be matched
                InputStreamReader brdr = new FileReader(fileIn);
                InputStreamReader frdr = new FileReader(fileOut);
	            DocumentComparator comp = new DocumentComparator(System.err);
	            if (comp.compare(frdr, brdr))
	            	getLog().info("JiBX document compare successful");
	            else
		            throw new MojoExecutionException("Documents are not equal on document-compare");	            	
            } catch (XmlPullParserException e) {
	            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
	            throw new MojoExecutionException(cause.getLocalizedMessage(), cause);
            } catch (FileNotFoundException e) {
	            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
	            throw new MojoExecutionException(cause.getLocalizedMessage(), cause);
            }
        	
        }
    }

    /**
     * Verifies the plugins configuration and sets default values if needed.
     * Note: Remember to call inherited methods first.
     */
    protected void checkConfiguration() {
    	super.checkConfiguration();
    }
    /**
     * Add the test class to the classpath.
     * Why isn't this done automatically?
     * @return The old classloader
     */
    private ClassLoader addTestClasspath()
    {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    	List<String> list = null;
    	try {
	        list = project.getTestClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
	        e.printStackTrace();
        }
        if ((list == null) || (list.size() == 0))
        	return oldClassLoader;
        ClassLoader parent = oldClassLoader;
        try {
            URL[] urls = new URL[list.size()];
            for (int i = 0; i < list.size(); i++)
            {
            	urls[i] = new File(list.get(i).toString()).toURI().toURL();
            }
//            { new File(getProjectBasedir(this.project) + "/target/test-classes").toURI().toURL() };
            if (parent == null) {
                parent = TestMultRoundtrip.class.getClassLoader();
            }
            ClassLoader loader = new URLClassLoader(urls, parent);
            Thread.currentThread().setContextClassLoader(loader);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        return oldClassLoader;
    }
    /**
     * Note: this is a hack to increase the visibility of runTest so I can call it.
     */
    public static class MyTestRoundtrip extends TestRoundtrip
    {
        /**
         * Run the comparison.
         */
        public static boolean runTest(String mname, String bname, String fin, String fout)
        	throws IOException, JiBXException, XmlPullParserException {
	            return TestRoundtrip.runTest(mname, bname, fin, fout);
        };    	
    }
    /**
     * Get the document files directory.
     * @return The document files directory.
     */
    protected String getXmlDirectory()
    {
    	return xmlDirectory;
    }
    /**
     * Get the document files directory.
     * @return The document files directory.
     */
    @Override
    String getDefaultSchemaBindingDirectory()
    {
    	return xmlDirectory;	// Never used
    }

    /**
     * Returns the build output directory of the given project.
     *
     * @throws  MojoExecutionException  if DependencyResolutionRequiredException occurs
     */
    protected Set<String> getProjectCompileClasspathElements(MavenProject project) throws MojoExecutionException {
        try {
            return new HashSet<String>(project.getCompileClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        }
    }
}
