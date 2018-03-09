package com.olf.jm.tranfieldutil.app;

import java.util.Arrays;

import com.olf.jm.tranfieldutil.model.OverriddenDefaultMetadata;
import com.olf.jm.tranfieldutil.model.TranFieldMetadata;


public class DefaultOverrider extends DefaultUtil {
	public DefaultOverrider () {
		super (OperationMode.DEFAULT_OVERRIDE, Arrays.asList((TranFieldMetadata[])OverriddenDefaultMetadata.values()));
	}
}
