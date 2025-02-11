package com.github.sergueik.selenium;

/**
 * Copyright 2022 Serguei Kouzmine
 */

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.openqa.selenium.devtools.v112.browser.Browser;
import org.openqa.selenium.devtools.v112.browser.model.Histogram;

/**
 * 
 * Selected test scenarios for Selenium 4 Chrome Developer Tools bridge
 * https://chromedevtools.github.io/devtools-protocol/tot/Browser/#method-getBrowserCommandLine
 * 
 * @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */

public class BrowserCommandLineDevToolsTest extends BaseDevToolsTest {

	private final static String url = "about:blank";
	private static List<String> results;

	@Test
	public void test() {
		// Act
		results = chromeDevTools.send(Browser.getBrowserCommandLine());
		// Assert
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThan(0));
		results.stream().forEach(o -> System.err.println(o));

	}

}
