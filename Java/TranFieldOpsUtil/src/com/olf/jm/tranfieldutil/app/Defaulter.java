package com.olf.jm.tranfieldutil.app;

import java.util.Arrays;

import com.olf.jm.tranfieldutil.model.DefaultingMetadata;

public class Defaulter extends DefaultUtil {
	public Defaulter () {
		super (OperationMode.DEFAULT_APPLY, Arrays.asList(DefaultingMetadata.values()));
	}
}
