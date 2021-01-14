package org.gds.totalbackup;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
//import org.apache.tools.ant.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Generates a list of files to cleanup in a specified directory, leaving behind the specified number
 *
 *  Sets the properties: string containing a comma separted list of files to delete
 *  Expects the property: file containing remote directory contents, # remaining files
 *
 * org.gds.totalbackup.CaGetRemoteSshFileList
 */
public class CaGetRemoteSshFileList extends Task
{
    private File _sshExecOutputFile;
	protected Integer _numberRemainingFiles;  
	
    public void setSshExecOutputFile(File sshExecOutputFile)
    {
    	_sshExecOutputFile = sshExecOutputFile;
    }	
  
    public void setNumberRemainingFiles(String numberRemainingFiles)
    {
    	_numberRemainingFiles = Integer.parseInt(numberRemainingFiles);
    }

    protected void assertion() throws BuildException
    {
		if( _sshExecOutputFile==null )
			throw new BuildException("The output file generated from sshexec must exist");
		if( _numberRemainingFiles==null )
			throw new BuildException("You must specify the number of remaining files");
	}

    public void execute()
    {
    	int fileCount = 1;  //There should always be at least one, that is, the file that was just transferred.
    	
		assertion();
			
		try {
			fileCount = getFileCount();
		}
		catch (Exception e)
		{
			log("Could not read the list of files from the ssh server");
		}
		
		String fileArray[] = new String[fileCount];
		String commaSepartedFileList = null;

		
		try {
			log("Generating comma separated file list...");
			
			//Generate an array of filenames from a file
    		buildFileList(fileArray);
    		
    		//Call outputArray - should display unsorted list
    		outputArray(fileArray);
    		
    		//Generate list of filenames to delete via ssh exec commands
    		commaSepartedFileList = generateRemoteSshFileList(fileArray);
		}
		catch (Exception e)
		{
			log("Could not read the list of files from the ssh server");
		}

		if (commaSepartedFileList == null) {
            log("No comma separated list of files was generated");
            getProject().setProperty("remoteFiles", "nofiles");
        }
        else {
            log("The comma separated list of files that will be cleaned up is: " + commaSepartedFileList);
            getProject().setProperty("remoteFiles", commaSepartedFileList);
        }
    }
    
    /*
     * output array should eventually be updated to use Generics, and factored out into a top level superclass
     * to be reused in other package classes such as CaFileCleanup
     */
    
    private void outputArray(String array[])
    {
        for (int i = 0; i < array.length; i++)		// loop through the array
	    {
        	log("The filename of file in position " + i + " of the array is " + array[i]);
	    }
    }    

    private int getFileCount() throws Exception
    {
    	int i = 0;
   	    try 
   	    {
      	    BufferedReader br = new BufferedReader(new FileReader(_sshExecOutputFile));

		    while ( br.readLine() != null ) {     //parse file and count the number of filenames
		    	i++;
		    }
		    br.close();
   	    }
		catch (Exception e)
		{
			log("There was an error reading from the file " +  _sshExecOutputFile);
			log(e.getMessage());
		}
		return i;
    }    
    
    
    private void buildFileList(String fileArray[]) throws Exception
    {
   	    try 
   	    {
      	    BufferedReader br = new BufferedReader(new FileReader(_sshExecOutputFile));
	    	String line = null;
		    int i = 0;
		    while ( (line = br.readLine()) != null ) {     //parse file and create the array of filenames
		    	fileArray[i] = line.trim();
		    	i++;
		    }
		    br.close();
   	    }
		catch (Exception e)
		{
			log("There was an error reading from the file " +  _sshExecOutputFile);
			log(e.getMessage());
		}
		return;
    }
    
    private String generateRemoteSshFileList(String fileArray[])
    {    	
    	int numberToDelete = fileArray.length - _numberRemainingFiles; 
    	
    	if (numberToDelete <= 0 ) 
    	{
    		log("Array contains " + fileArray.length + " filenames and " + _numberRemainingFiles + " requested to remain");
    		return null;	
    	}
    	else {
    		String deleteList = null;
    		int i = 0;
    		log("Adding filename " + fileArray[i] + " in position " + i + " of the array to the list of files to delete");
    		deleteList = fileArray[i];
    		
        	for (i = 1; i < numberToDelete; i++)
	        {
        	    log("Adding filename " + fileArray[i] + " in position " + i + " of the array to the list of files to delete");
        	    deleteList = deleteList + "," + fileArray[i];
	        }
        	return deleteList;
        }
    }
}

