package com.jm.eod.common;

import com.olf.openjvs.enums.COL_TYPE_ENUM;

    public class Parameters {
    	
    	private final String name;
    	private final COL_TYPE_ENUM type;
    	private final String value;
    	
		public Parameters(String name, COL_TYPE_ENUM type, String value) {
			this.name = name;
			this.type = type;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public COL_TYPE_ENUM getType() {
			return type;
		}

		public String getValue() {
			return value;
		}
    	    	
    
}
