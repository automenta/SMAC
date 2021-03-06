package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.io.File;

/**
 * JCommander Validator that determines whether or not a directory is writable
 * @author sjr
 *
 */
public class ReadableDirectoryValidator implements IStringConverter<File> , DomainDisplay {

	  public File convert(String value) {
		 
		File f = new File(value);
		
		if (!f.isDirectory())
		{
			throw new ParameterException(value + " is not a directory");
		}
		
		if (!f.canRead())
		{
			throw new ParameterException(value + " is not a readable directory");
		}
		
		return f;
	     
	  }

	@Override
	public String getDomain() {
		return "{ readable directories }";
	} 

}
