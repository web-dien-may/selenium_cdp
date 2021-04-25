package com.github.sergueik.selenium;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Selected test scenarios for Selenium 4 Chrome Developer Tools bridge inspired
 * by https://toster.ru/q/653249?e=7897302#comment_1962398
 * https://chromedevtools.github.io/devtools-protocol/tot/Network/#method-setUserAgentOverride
 * @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */

public class UserAgentOverrideCdpTest extends BaseCdpTest {

	private static WebElement element = null;
	private static By locator = null;
	private static Map<String, Object> customAgent = new HashMap<>();

	static {
		{
			customAgent.put("userAgent", "python 2.7");
			customAgent.put("platform", "Windows");
		}
	};

	@After
	public void clearPage() {
		driver.get("about:blank");
	}

	// NOTE:
	// the URL used in this test shows bogus information about
	// the sender ip address (not confirmed)
	// and does not reflect the change in the user agent made via CDP
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-setUserAgentOverride
	// https://stackoverflow.com/questions/29916054/change-user-agent-for-selenium-driver
	@Ignore

	@Test
	public void test1() {
		// Arrange
		driver.get("https://www.whoishostingthis.com/tools/user-agent/");
		locator = By.cssSelector("div.info-box.user-agent");
		element = driver.findElement(locator);
		Utils.highlight(element);
		Utils.sleep(100);
		assertThat(element.getAttribute("innerText"), containsString("Mozilla"));

		// Act
		try {
			driver.executeCdpCommand("Network.setUserAgentOverride", customAgent);
		} catch (WebDriverException e) {
			System.err.println("Web Driver exception (ignored): "
					+ Utils.processExceptionMessage(e.getMessage()));
		} catch (Exception e) {
			System.err.println("Exception " + e.toString());
			throw (new RuntimeException(e));
		}
		driver.navigate().refresh();
		Utils.sleep(2000);

		element = driver.findElement(locator);
		assertThat(element.isDisplayed(), is(true));
		assertThat(element.getAttribute("innerText"), is("python 2.7"));
	}

	@Test
	public void test2() {
		// Arrange
		driver.get(
				"https://www.whatismybrowser.com/detect/what-http-headers-is-my-browser-sending");
		locator = By.xpath(
				"//*[@id=\"content-base\"]//table//th[contains(text(),\"USER-AGENT\")]/../td");
		element = driver.findElement(locator);
		Utils.highlight(element);
		Utils.sleep(100);
		assertThat(element.getAttribute("innerText"), containsString("Mozilla"));
		System.err
				.println("Vanilla USER-AGENT: " + element.getAttribute("innerText"));

		// Act
		try {
			driver.executeCdpCommand("Network.setUserAgentOverride", customAgent);
		} catch (WebDriverException e) {
			System.err.println("Web Driver exception (ignored): "
					+ Utils.processExceptionMessage(e.getMessage()));
		} catch (Exception e) {
			System.err.println("Exception " + e.toString());
			throw (new RuntimeException(e));
		}
		driver.navigate().refresh();
		Utils.sleep(1000);

		element = driver.findElement(locator);
		assertThat(element.isDisplayed(), is(true));
		assertThat(element.getAttribute("innerText"), is("python 2.7"));
		System.err
				.println("Updated USER-AGENT: " + element.getAttribute("innerText"));
	}
}
