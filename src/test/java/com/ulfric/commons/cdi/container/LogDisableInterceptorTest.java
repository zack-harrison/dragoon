package com.ulfric.commons.cdi.container;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
public class LogDisableInterceptorTest extends StateInterceptorTestBase<LogDisableInterceptor> {

	LogDisableInterceptorTest()
	{
		super(new LogDisableInterceptor());
	}

	@Test
	void testBefore_noExceptions()
	{
		this.before_noExceptions();
	}

	@Test
	void testAfter_noExceptions()
	{
		this.after_noExceptions();
	}

}
