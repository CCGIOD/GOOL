package gool.generator.python;

import gool.ast.constructs.ClassDef;
import gool.generator.common.CodePrinter;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import logger.Log;

public class PythonCodePrinter extends CodePrinter {
	
	public PythonCodePrinter(File outputDir, Collection<File> myF) {
		super(new PythonGenerator(), outputDir, myF);
	}
	
	@Override
	public String getFileName(String className) {
		return className + ".py";
	}

	@Override
	public String getTemplateDir() {
		return "";
	}

	@Override
	public List<File> print(ClassDef pclass) throws FileNotFoundException {
		List<File> res = super.print(pclass);
		
		createInitFile(getOutputDir());
		
		return res;		
	}
	
	private void createInitFile(File dir) {
		File[] dirs = dir.listFiles(new FileFilter(){
		  public boolean accept(File f) {
		    return f.isDirectory();
		  }
		});
		
		for(File d : dirs) {
			File init = new File(d, "__init__.py");
			try {
				init.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(e);
			}
			createInitFile(d);
		}
	}
}
