package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.DateField;

public class DateFieldImpl implements DateField {

	private String date;

	@Override
	public String getDate() {
		return date;
	}

	@Override
	public DateField setDate(String date) {
		this.date = date;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}

	@Override
	public String toString() {
		return String.valueOf(getDate());
	}
}
