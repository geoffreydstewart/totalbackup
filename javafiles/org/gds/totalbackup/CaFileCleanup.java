package org.gds.totalbackup;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FilenameFilter;

/** Performs a cleanup of files in a specified directory, leaving behind the specified number
 *
 *  Sets the properties:
 *  Expects the property: cleanup location, # remaining files
 *
 * org.gds.totalbackup.CaFileCleanup
 */
public class CaFileCleanup extends Task
{
    private File _fileCleanupLocation;
	protected Integer _numberRemainingFiles; 
	
    public void setFileCleanupLocation(File fileCleanupLocation)
    {
    	_fileCleanupLocation = fileCleanupLocation;
    }

    public void setNumberRemainingFiles(String numberRemainingFiles)
    {
    	_numberRemainingFiles = Integer.parseInt(numberRemainingFiles);
    }

    protected void assertion() throws BuildException
    {
		if( _fileCleanupLocation==null )
			throw new BuildException("You must specify a file location");
		if( _numberRemainingFiles==null )
			throw new BuildException("You must specify the number of remaining files");
	}

    public void execute() throws BuildException
    {
		assertion();
		//String file_cleanup_list[] = _file_cleanup_location.list();
		File fileList[] = _fileCleanupLocation.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".zip");
		    }
		});
		
		//Call outputArray - should display unsorted list
		log("Unsorted Array...");
		outputArray(fileList);
        
		//Sort Array
		sortFileArrayByModifiedDate(fileList);
	
		//Call outputArray - should display sorted list
		log("Sorted Array...");
		outputArray(fileList);

        performFileCleanup(fileList);
		
        //Refresh directory listing
        File cleanedFileList[] = _fileCleanupLocation.listFiles();
        
      //Sort Array
        sortFileArrayByModifiedDate(cleanedFileList);
        
        //Call outputArray - should display sorted list
		log("Cleaned Array...");
		outputArray(cleanedFileList);        
		
        //Sample
		//getProject().setProperty("propertyToSet", actualVariable);
    
    }
    
    
    /*
     * output array should eventually be updated to use Generics, and factored out into a top level superclass
     * to be reused in other package classes such as CaGetRemoteSshFileList
     */
    
    private void outputArray(File array[])
    {
        for (int i = 0; i < array.length; i++)		// loop through the unsorted array
	    {
    	    log("The filename of file in position " + i + " of the array is " + array[i].getName());
	    }
    }
    
    private void sortFileArrayByModifiedDate(File array[])  //implemented with an insertionSort
    // pre: array is full, all File elements are not null
    // post: the array is sorted in order of oldest to newest
    {
 	    long temp;     // this variable is the lastModified value for the element of the unsorted array currently
 				       // being compared to elements of the sorted array
 	    File tempFile; // this variable holds the actual File which is the element of the unsorted array currently
	                   // being compared to elements of the sorted array
 	    int pos;       // this variable keeps track of where in the sorted array the 
 				       // comparison takes place
 	
        for (int i = 1; i < array.length; i++)		// loop through the unsorted array
 	    {
 		                                                // (the first element is considered sorted)
 		    temp = array[i].lastModified();	            // grab the first element of the unsorted array
 		    tempFile = array[i];
 		    pos = i - 1;                                // get the index of the last sorted element
 		
 		    while ((pos >= 0) && (temp < array [pos].lastModified()))	// while the current sorted element is greater than temp
 		    {
 			    array[pos + 1] = array[pos];	        // keep shifting sorted elements back by 1
 			    pos--;                                  // decrement the pos index
 		    }
 		    array[pos + 1] = tempFile;                  // insert temp such that array[pos] <= temp < array[pos + 2]
 	    }
    }

    private void performFileCleanup(File array[])
    {
    	
    	int numberToDelete = array.length - _numberRemainingFiles; 
    	
    	if (numberToDelete < 0 ) 
    	{
    		log("Directory contains " + array.length + " files and " + _numberRemainingFiles + " requested to remain");
    		log("No cleanup performed!");
    		return;	
    	}
    	else {
        	for (int i = 0; i < numberToDelete; i++)		// loop through the unsorted array
	        {
        	    log("Deleting file " + array[i].getName() + " in position " + i + " of the array");
        	    if (array[i].delete()) log("File " + array[i].getName() + " was successfully deleted");
	        }
        	return;
        }
    }

}

