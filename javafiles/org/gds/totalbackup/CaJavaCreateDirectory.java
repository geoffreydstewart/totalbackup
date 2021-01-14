package org.gds.totalbackup;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;

/** Simply creates the specified directory, if possible, and returns gracefully in the event of error
 *
 *  Sets the properties: string flag if the directory was successfully created
 *  Expects the properties: parent directory, and directory name which is intended to be created
 *
 * org.gds.totalbackup.CaJavaCreateDirectory
 */
public class CaJavaCreateDirectory extends Task
{
    private String _directory;
    private String _parentDirectory;

    public void setDirectory(String directory)
    {
    	_directory = directory;
    }

    public void setParentDirectory(String parentDirectory)
    {
    	_parentDirectory = parentDirectory;
    }

    private String getDirectory()
    {
    	return _directory;
    }

    private String getParentDirectory()
    {
    	return _parentDirectory;
    }

    private void assertion() throws BuildException
    {
		if( getDirectory() == null || getParentDirectory() == null)
			throw new BuildException("A directory must be specified");
	}

    public void execute()
    {
		assertion();
		String parentDirectory = getParentDirectory();
		String directory = getDirectoryName();
		File dir = null;
		
		boolean directoryCreated = false;

		File parentDir = new File(parentDirectory);
		try 
		{
		    if (parentDir.exists())
			{
		    	dir = new File(parentDirectory + File.pathSeparator + directory);
				directoryCreated = dir.mkdir();
			}
			else 
			{
				if (parentDir.mkdirs())
				{
					dir = new File(parentDirectory + File.pathSeparator + directory);
					directoryCreated = dir.mkdir();
				}
			}
		}
		catch (SecurityException se)
		{
			log("WARNING!  There was a permissions problem while creating the directory " + directory);
		}
		
		if (directoryCreated)
		{
			getProject().setProperty("directoryCreated", "true");
		}
		else
		{
			getProject().setProperty("directoryCreated", "false");
		}
    }

    private String getDirectoryName()
    {
    	String directoryName = getDirectory();
    	int indexOfRightmostSlash = directoryName.lastIndexOf("/");
    	if (indexOfRightmostSlash > 0)
    	{
    	    directoryName = directoryName.substring(indexOfRightmostSlash+1);
    	}
    	return directoryName;
    }
}

