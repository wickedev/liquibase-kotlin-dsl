package org.liquibase.groovy.helper

import liquibase.resource.ClassLoaderResourceAccessor

public class JUnitResourceAccessor extends ClassLoaderResourceAccessor {

	public JUnitResourceAccessor(urls) throws Exception {
		super(new URLClassLoader(urls as URL[]), JUnitResourceAccessor.class.getClassLoader());
	}
}
