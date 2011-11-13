package org.jibx.maven;

import java.util.List;

public class IncludeBaseBinding
{
    public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}

	/**
     * @parameter
     */
    String groupId;

    /**
     * @parameter
     */
    String artifactId;

    /**
     * @parameter
     */
    String version;

    /**
     * @parameter
     */
    String classifier;

    /**
     * @parameter
     */
    String directory;

    /**
     * @parameter
     */
    List<String> includes;

}
