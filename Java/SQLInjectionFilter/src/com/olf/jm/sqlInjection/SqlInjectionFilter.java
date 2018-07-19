package com.olf.jm.sqlInjection;

public class SqlInjectionFilter {

	 private static String[] keyWords = { ";", "\"", "\'", "/*", "*/", "--", "exec",
         "select", "update", "delete", "insert",
         "alter", "drop", "create", "shutdown" }; 
	 
	 
	public void doFilter(String parameter) throws SqlInjectionException{
	    String lowerCase = parameter.toLowerCase();
	    for (int i = 0; i < keyWords.length; i++) {
	      if (lowerCase.indexOf(keyWords[i]) >= 0) {
	    	  StringBuffer sb = new StringBuffer();
	    	  
	    	  sb.append("Possible SQL injection. Found keyword ");
	    	  sb.append(keyWords[i]);
	    	  sb.append(" in the parameter ");
	    	  sb.append(parameter);

	    	  throw new SqlInjectionException(sb.toString());
	      }
	    }
		
	}
}
